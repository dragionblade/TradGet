package com.example.tradget;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a Google Maps encoded polyline string into a list of LatLng points.
 *
 * <p>Algorithm reference:
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 *
 * <p>This avoids a dependency on the Maps SDK utility library while still correctly
 * handling the full encoding specification including negative coordinates.
 */
public class PolylineDecoder {

    private PolylineDecoder() {
        // Static utility — no instances
    }

    /**
     * Decodes an encoded polyline string and returns the corresponding list of LatLng points.
     *
     * @param encoded the encoded polyline string from a Directions API response
     * @return ordered list of LatLng points forming the route
     */
    public static List<LatLng> decode(String encoded) {
        List<LatLng> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return points;

        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            // Decode latitude delta
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            // Decode longitude delta
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            points.add(new LatLng(lat / 1e5, lng / 1e5));
        }

        return points;
    }
}
