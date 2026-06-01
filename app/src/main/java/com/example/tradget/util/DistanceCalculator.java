package com.example.tradget.util;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class to calculate distance between two coordinates using Google Directions API.
 * Provides both distance (km) and duration (minutes).
 */
public class DistanceCalculator {

    private static final String DIRECTIONS_API_URL = 
        "https://maps.googleapis.com/maps/api/directions/json";

    public interface DistanceCallback {
        void onSuccess(double distanceKm, String durationText);
        void onError(String errorMessage);
    }

    /**
     * Returns straight-line distance in kilometers using the Haversine formula.
     * Useful as a fallback when the Directions API is unavailable.
     */
    public static double computeHaversineKm(double fromLat, double fromLng,
                                            double toLat, double toLng) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(toLat - fromLat);
        double dLng = Math.toRadians(toLng - fromLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(fromLat))
                * Math.cos(Math.toRadians(toLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    /**
     * Calculates distance and duration between two coordinates.
     * @param context Application context
     * @param fromLat Origin latitude
     * @param fromLng Origin longitude
     * @param toLat Destination latitude
     * @param toLng Destination longitude
     * @param apiKey Google Directions API key
     * @param callback Callback to receive distance and duration
     */
    public static void calculateDistance(Context context,
                                         double fromLat, double fromLng,
                                         double toLat, double toLng,
                                         String apiKey,
                                         DistanceCallback callback) {

        String url = DIRECTIONS_API_URL + 
            "?origin=" + fromLat + "," + fromLng +
            "&destination=" + toLat + "," + toLng +
            "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest request = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray routes = json.getJSONArray("routes");
                    
                    if (routes.length() == 0) {
                        callback.onError("No route found");
                        return;
                    }

                    JSONObject route = routes.getJSONObject(0);
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);

                    // Extract distance in meters and convert to km
                    JSONObject distance = leg.getJSONObject("distance");
                    int distanceMeters = distance.getInt("value");
                    double distanceKm = distanceMeters / 1000.0;

                    // Extract duration
                    JSONObject duration = leg.getJSONObject("duration");
                    String durationText = duration.getString("text");

                    callback.onSuccess(distanceKm, durationText);

                } catch (JSONException e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            },
            error -> callback.onError("Network error: " + error.getMessage())
        );

        queue.add(request);
    }
}
