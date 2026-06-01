package com.example.tradget;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Helper that fetches a driving route from the Google Directions REST API and
 * delivers the result back on the main thread.
 *
 * <p>Usage:
 * <pre>
 *   DirectionsHelper.fetchRoute(origin, dest, apiKey, new DirectionsHelper.RouteCallback() {
 *       public void onSuccess(RouteResult result) { ... }
 *       public void onFailure(String error)       { ... }
 *   });
 * </pre>
 */
public class DirectionsHelper {

    private static final String TAG = "DirectionsHelper";
    private static final String BASE_URL =
            "https://maps.googleapis.com/maps/api/directions/json";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /** Immutable result delivered to the caller after a successful route fetch. */
    public static class RouteResult {
        /** Decoded polyline points for the full overview route. */
        public final List<LatLng> polyline;
        /** Human-readable distance string, e.g. "12.3 km". */
        public final String distanceText;
        /** Human-readable travel time string, e.g. "23 mins". */
        public final String durationText;
        /** Distance in metres. */
        public final int distanceMetres;

        RouteResult(List<LatLng> polyline, String distanceText,
                    String durationText, int distanceMetres) {
            this.polyline       = polyline;
            this.distanceText   = distanceText;
            this.durationText   = durationText;
            this.distanceMetres = distanceMetres;
        }
    }

    /** Callback interface — methods are always invoked on the main thread. */
    public interface RouteCallback {
        void onSuccess(RouteResult result);
        void onFailure(String errorMessage);
    }

    /**
     * Fetches a driving route between {@code origin} and {@code destination} asynchronously.
     *
     * @param origin      start LatLng
     * @param destination end LatLng
     * @param apiKey      Google Maps API key (must have Directions API enabled)
     * @param callback    result delivered on the main thread
     */
    public static void fetchRoute(LatLng origin, LatLng destination,
                                   String apiKey, RouteCallback callback) {

        String url = String.format(Locale.US,
                "%s?origin=%f,%f&destination=%f,%f&mode=driving&key=%s",
                BASE_URL,
                origin.latitude, origin.longitude,
                destination.latitude, destination.longitude,
                apiKey);

        EXECUTOR.execute(() -> {
            Request request = new Request.Builder().url(url).build();
            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    deliverFailure(callback, "HTTP error: " + response.code());
                    return;
                }

                String body = response.body().string();
                JSONObject json = new JSONObject(body);

                String status = json.optString("status", "");
                String errorMessage = json.optString("error_message", "");
                if (!"OK".equals(status)) {
                    String detail = errorMessage.isEmpty() ? "" : " (" + errorMessage + ")";
                    deliverFailure(callback, "Directions API status: " + (status.isEmpty() ? "MISSING_STATUS" : status) + detail);
                    return;
                }

                JSONArray routes = json.optJSONArray("routes");
                if (routes == null || routes.length() == 0) {
                    deliverFailure(callback, "Directions API returned no routes");
                    return;
                }

                JSONObject leg = routes.getJSONObject(0)
                        .getJSONArray("legs").getJSONObject(0);

                String distText = leg.getJSONObject("distance").getString("text");
                String durText  = leg.getJSONObject("duration").getString("text");
                int distMetres  = leg.getJSONObject("distance").getInt("value");

                String encodedPoly = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points");

                List<LatLng> polyline = PolylineDecoder.decode(encodedPoly);

                RouteResult result = new RouteResult(polyline, distText, durText, distMetres);
                MAIN_HANDLER.post(() -> callback.onSuccess(result));

            } catch (Exception e) {
                Log.e(TAG, "Error fetching route", e);
                deliverFailure(callback, e.getMessage());
            }
        });
    }

    private static void deliverFailure(RouteCallback callback, String message) {
        MAIN_HANDLER.post(() -> callback.onFailure(message));
    }
}
