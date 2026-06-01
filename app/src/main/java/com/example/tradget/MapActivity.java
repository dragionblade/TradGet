package com.example.tradget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.tradget.util.CostCalculator;
import com.example.tradget.util.DistanceCalculator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;
import java.util.Locale;

/**
 * Full-featured map activity that:
 *  1. Receives origin/destination from {@link HomeFragment} via Intent extras.
 *  2. Places custom markers for both locations.
 *  3. Calls the Google Directions REST API to fetch a driving route.
 *  4. Draws the decoded polyline on the map in TradGet cyan.
 *  5. Fits the camera to show the complete route.
 *  6. Tracks the user's live position and updates a blue-dot marker.
 *  7. Shows distance, ETA, and a real-time fuel cost split calculator.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── Intent extra keys ──────────────────────────────────────────────────────
    public static final String EXTRA_FROM_LAT  = "from_lat";
    public static final String EXTRA_FROM_LNG  = "from_lng";
    public static final String EXTRA_FROM_NAME = "from_name";
    public static final String EXTRA_TO_LAT    = "to_lat";
    public static final String EXTRA_TO_LNG    = "to_lng";
    public static final String EXTRA_TO_NAME   = "to_name";
    public static final String EXTRA_MODE      = "mode";

    // ── Constants ──────────────────────────────────────────────────────────────
    /** Average bike fuel efficiency (km/litre) for cost estimation. */
    private static final double FUEL_EFFICIENCY_KPL = 40.0;
    /** Default fuel price in ₹/litre. */
    private static final float DEFAULT_FUEL_PRICE = 95f;
    /** Route polyline colour — TradGet brand cyan. */
    private static final int ROUTE_COLOR = Color.parseColor("#00BCD4");
    /** Live-location update interval (milliseconds). */
    private static final long LOCATION_INTERVAL_MS = 4000;

    private static final int PERM_REQUEST_CODE = 200;

    // ── Map ────────────────────────────────────────────────────────────────────
    private GoogleMap mMap;
    private Polyline  mRoutePolyline;
    private Marker    mOriginMarker;
    private Marker    mDestinationMarker;
    private Marker    mMyLocationMarker;
    private Circle    mMyLocationCircle;

    // ── Route data ─────────────────────────────────────────────────────────────
    private LatLng mOrigin;
    private LatLng mDestination;
    private String mOriginName;
    private String mDestinationName;
    private String mMode;
    private int    mDistanceMetres = 0;

    // ── Live location ──────────────────────────────────────────────────────────
    private FusedLocationProviderClient mFusedClient;
    private LocationCallback            mLocationCallback;

    // ── UI ─────────────────────────────────────────────────────────────────────
    private TextView    tvRouteTitle, tvModeLabel;
    private TextView    tvDistance, tvDuration;
    private TextView    tvCostPerPerson;
    private TextView    tvPassengersCount;
    private EditText    etFuelPrice;
    private LinearLayout mapBottomCard, mapLoadingOverlay;

    private int mPassengerCount = 2;

    // ══════════════════════════════════════════════════════════════════════════
    // Activity lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // ── Parse intent extras ────────────────────────────────────────────────
        double fromLat  = getIntent().getDoubleExtra(EXTRA_FROM_LAT,  0);
        double fromLng  = getIntent().getDoubleExtra(EXTRA_FROM_LNG,  0);
        double toLat    = getIntent().getDoubleExtra(EXTRA_TO_LAT,    0);
        double toLng    = getIntent().getDoubleExtra(EXTRA_TO_LNG,    0);
        mOriginName      = getIntent().getStringExtra(EXTRA_FROM_NAME);
        mDestinationName = getIntent().getStringExtra(EXTRA_TO_NAME);
        mMode            = getIntent().getStringExtra(EXTRA_MODE);

        mOrigin      = new LatLng(fromLat, fromLng);
        mDestination = new LatLng(toLat,   toLng);

        // ── Bind views ─────────────────────────────────────────────────────────
        bindViews();
        setupHeaderButtons();
        setupFuelSplitListeners();

        // ── Initialise location client ─────────────────────────────────────────
        mFusedClient = LocationServices.getFusedLocationProviderClient(this);

        // ── Load map asynchronously ────────────────────────────────────────────
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLiveLocationUpdates();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // View binding helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        tvRouteTitle      = findViewById(R.id.mapRouteTitle);
        tvModeLabel       = findViewById(R.id.mapModeLabel);
        tvDistance        = findViewById(R.id.mapDistance);
        tvDuration        = findViewById(R.id.mapDuration);
        tvCostPerPerson   = findViewById(R.id.costPerPerson);
        tvPassengersCount = findViewById(R.id.passengersCount);
        etFuelPrice       = findViewById(R.id.fuelPriceInput);
        mapBottomCard     = findViewById(R.id.mapBottomCard);
        mapLoadingOverlay = findViewById(R.id.mapLoadingOverlay);

        tvRouteTitle.setText(shortName(mOriginName) + " → " + shortName(mDestinationName));
        tvModeLabel.setText(mMode != null ? mMode : "Passenger");
        etFuelPrice.setText(String.valueOf((int) DEFAULT_FUEL_PRICE));
    }

    private void setupHeaderButtons() {
        findViewById(R.id.mapBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.mapRecenterButton).setOnClickListener(v -> recenterCamera());
    }

    private void setupFuelSplitListeners() {
        // Passenger +/– stepper
        TextView decrease = findViewById(R.id.passengersDecrease);
        TextView increase = findViewById(R.id.passengersIncrease);

        decrease.setOnClickListener(v -> {
            if (mPassengerCount > 1) {
                mPassengerCount--;
                tvPassengersCount.setText(String.valueOf(mPassengerCount));
                recalculateCost();
            }
        });

        increase.setOnClickListener(v -> {
            if (mPassengerCount < 8) {
                mPassengerCount++;
                tvPassengersCount.setText(String.valueOf(mPassengerCount));
                recalculateCost();
            }
        });

        // Recalculate whenever fuel price changes
        etFuelPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { recalculateCost(); }
        });

        // Confirm button
        findViewById(R.id.confirmRideBtn).setOnClickListener(v ->
                Toast.makeText(this,
                        "Ride confirmed! ₹" + getCostPerPerson() + " per person",
                        Toast.LENGTH_LONG).show());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OnMapReadyCallback
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        configureMap();
        placeMarkers();
        showLoadingIndicator(true);
        fetchAndDrawRoute();
        requestLiveLocationUpdates();
    }

    private void configureMap() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false); // We have our own toolbar
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Markers
    // ══════════════════════════════════════════════════════════════════════════

    private void placeMarkers() {
        // Origin — green hue marker
        mOriginMarker = mMap.addMarker(new MarkerOptions()
                .position(mOrigin)
                .title(mOriginName)
                .snippet("Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Destination — red (default) marker
        mDestinationMarker = mMap.addMarker(new MarkerOptions()
                .position(mDestination)
                .title(mDestinationName)
                .snippet("Drop-off")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Show info windows by default
        if (mOriginMarker      != null) mOriginMarker.showInfoWindow();
        if (mDestinationMarker != null) mDestinationMarker.showInfoWindow();

        // Move camera to midpoint initially
        LatLng midPoint = midpoint(mOrigin, mDestination);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(midPoint, 12));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Route fetching & drawing
    // ══════════════════════════════════════════════════════════════════════════

    private void fetchAndDrawRoute() {
        DirectionsHelper.fetchRoute(
                mOrigin,
                mDestination,
                BuildConfig.MAPS_API_KEY,
                new DirectionsHelper.RouteCallback() {

                    @Override
                    public void onSuccess(DirectionsHelper.RouteResult result) {
                        showLoadingIndicator(false);
                        drawPolyline(result.polyline);
                        fitCameraToRoute(result.polyline);
                        updateInfoCard(result.distanceText, result.durationText);
                        mDistanceMetres = result.distanceMetres;
                        recalculateCost();
                        mapBottomCard.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoadingIndicator(false);
                        Toast.makeText(MapActivity.this,
                                "Could not fetch route: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                        // Fall back to straight-line distance
                        double fallbackKm = DistanceCalculator.computeHaversineKm(
                                mOrigin.latitude, mOrigin.longitude,
                                mDestination.latitude, mDestination.longitude);
                        mDistanceMetres = (int) Math.round(fallbackKm * 1000.0);
                        updateInfoCard(String.format(Locale.getDefault(), "%.1f km", fallbackKm), "N/A");
                        recalculateCost();
                        mapBottomCard.setVisibility(View.VISIBLE);
                        // Fall back: fit camera to just the two markers
                        fitCameraToMarkers();
                    }
                });
    }

    private void drawPolyline(List<LatLng> points) {
        if (mRoutePolyline != null) {
            mRoutePolyline.remove();
        }
        mRoutePolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(ROUTE_COLOR)
                .width(10f)
                .geodesic(true));
    }

    /** Animates the camera to fit the full route polyline with comfortable padding. */
    private void fitCameraToRoute(List<LatLng> points) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            boundsBuilder.include(point);
        }
        LatLngBounds bounds = boundsBuilder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
    }

    /** Fallback camera fit when no polyline is available. */
    private void fitCameraToMarkers() {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(mOrigin)
                .include(mDestination)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Info card & fuel cost split
    // ══════════════════════════════════════════════════════════════════════════

    private void updateInfoCard(String distance, String duration) {
        tvDistance.setText(distance);
        tvDuration.setText(duration);
    }

    private void recalculateCost() {
        double fuelPrice;
        try {
            fuelPrice = Double.parseDouble(etFuelPrice.getText().toString().trim());
        } catch (NumberFormatException e) {
            fuelPrice = DEFAULT_FUEL_PRICE;
        }

        if (mDistanceMetres <= 0 || mPassengerCount <= 0) {
            tvCostPerPerson.setText("₹ —");
            return;
        }

        double distanceKm = mDistanceMetres / 1000.0;
        double costPerPerson = CostCalculator.calculateCostPerPerson(
            distanceKm, fuelPrice, mPassengerCount);

        tvCostPerPerson.setText(costPerPerson > 0
            ? CostCalculator.formatCost(costPerPerson)
            : "₹ —");
    }

    private double getCostPerPerson() {
        String raw = tvCostPerPerson.getText().toString()
                .replace("₹", "").replace("—", "0").trim();
        try { return Double.parseDouble(raw); } catch (NumberFormatException e) { return 0; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Live location tracking
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressLint("MissingPermission")
    private void requestLiveLocationUpdates() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERM_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(LOCATION_INTERVAL_MS)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(2000)
                .build();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                android.location.Location loc = result.getLastLocation();
                if (loc != null) {
                    updateMyLocationMarker(new LatLng(loc.getLatitude(), loc.getLongitude()));
                }
            }
        };

        mFusedClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
    }

    private void stopLiveLocationUpdates() {
        if (mFusedClient != null && mLocationCallback != null) {
            mFusedClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /** Updates (or creates) the live-location blue dot marker. */
    private void updateMyLocationMarker(LatLng position) {
        if (mMap == null) return;

        if (mMyLocationMarker == null) {
            mMyLocationMarker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("You are here")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE))
                    .zIndex(1f)); // On top of other markers

            // Accuracy circle
            mMyLocationCircle = mMap.addCircle(new CircleOptions()
                    .center(position)
                    .radius(30)
                    .strokeColor(Color.parseColor("#4400BCD4"))
                    .fillColor(Color.parseColor("#2200BCD4"))
                    .strokeWidth(2f));
        } else {
            mMyLocationMarker.setPosition(position);
            mMyLocationCircle.setCenter(position);
        }
    }

    private void recenterCamera() {
        if (mMyLocationMarker != null) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(mMyLocationMarker.getPosition())
                            .zoom(16f)
                            .build()));
        } else {
            fitCameraToMarkers();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Permissions
    // ══════════════════════════════════════════════════════════════════════════

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLiveLocationUpdates();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI utilities
    // ══════════════════════════════════════════════════════════════════════════

    private void showLoadingIndicator(boolean show) {
        mapLoadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Returns up to 20 chars of a place name, truncated with "…" if needed. */
    private static String shortName(String name) {
        if (name == null) return "";
        return name.length() > 20 ? name.substring(0, 17) + "…" : name;
    }

    /** Computes the geographic midpoint between two LatLng coordinates. */
    private static LatLng midpoint(LatLng a, LatLng b) {
        return new LatLng((a.latitude + b.latitude) / 2.0,
                          (a.longitude + b.longitude) / 2.0);
    }
}