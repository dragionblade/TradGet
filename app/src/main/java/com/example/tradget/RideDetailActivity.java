package com.example.tradget;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradget.model.RideRequest;
import com.example.tradget.util.CostCalculator;
import com.example.tradget.ErrorHandler;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shows full details of a specific ride offer and lets the passenger send a request.
 * After the request is sent, attaches a real-time Firestore listener to show status updates.
 */
public class RideDetailActivity extends AppCompatActivity {

    // ── Intent extras ──────────────────────────────────────────────────────────
    public static final String EXTRA_RIDE_ID       = "ride_id";
    public static final String EXTRA_RIDER_ID      = "rider_id";
    public static final String EXTRA_RIDER_NAME    = "rider_name";
    public static final String EXTRA_RIDER_RATING  = "rider_rating";
    public static final String EXTRA_VEHICLE       = "vehicle";
    public static final String EXTRA_FROM_NAME     = "from_name";
    public static final String EXTRA_TO_NAME       = "to_name";
    public static final String EXTRA_FROM_LAT      = "from_lat";
    public static final String EXTRA_FROM_LNG      = "from_lng";
    public static final String EXTRA_TO_LAT        = "to_lat";
    public static final String EXTRA_TO_LNG        = "to_lng";
    public static final String EXTRA_COST          = "cost";
    public static final String EXTRA_SEATS         = "seats";
    public static final String EXTRA_DEPARTURE_MS  = "departure_ms";
    public static final String EXTRA_DISTANCE_KM   = "distance_km";
    public static final String EXTRA_FUEL_PRICE    = "fuel_price";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView     detailRiderInitial, detailRiderName, detailVehicle, detailRating;
    private TextView     detailFromName, detailToName, detailCost, detailSeats, detailTime;
    private TextView     detailDistance, detailFuelCostBreakdown;
    private TextView     requestBtnLabel, requestStatusText;
    private TextView     cancelRequestButton;
    private LinearLayout requestRideBtn, requestStatusBanner;
    private ProgressBar  requestProgress, requestStatusProgress;

    // ── State ──────────────────────────────────────────────────────────────────
    private String rideId, riderId, riderName, fromName, toName, vehicle;
    private double cost, fromLat, fromLng, toLat, toLng, distanceKm = 0, fuelPrice = 95.0;
    private int    seats;
    private long   departureMs;
    private String activeRequestId;
    private ListenerRegistration requestListener;

    // ── Services ───────────────────────────────────────────────────────────────
    private SessionManager     session;
    private FirestoreRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_detail);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

        session = new SessionManager(this);
        repo    = FirestoreRepository.getInstance();

        parseExtras();
        bindViews();
        populateUI();
        setupClickListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestListener != null) requestListener.remove();
    }

    private void parseExtras() {
        Intent i     = getIntent();
        rideId       = i.getStringExtra(EXTRA_RIDE_ID);
        riderId      = i.getStringExtra(EXTRA_RIDER_ID);
        riderName    = i.getStringExtra(EXTRA_RIDER_NAME);
        vehicle      = i.getStringExtra(EXTRA_VEHICLE);
        fromName     = i.getStringExtra(EXTRA_FROM_NAME);
        toName       = i.getStringExtra(EXTRA_TO_NAME);
        fromLat      = i.getDoubleExtra(EXTRA_FROM_LAT, 0);
        fromLng      = i.getDoubleExtra(EXTRA_FROM_LNG, 0);
        toLat        = i.getDoubleExtra(EXTRA_TO_LAT,   0);
        toLng        = i.getDoubleExtra(EXTRA_TO_LNG,   0);
        cost         = i.getDoubleExtra(EXTRA_COST,     0);
        seats        = i.getIntExtra(EXTRA_SEATS,        1);
        departureMs  = i.getLongExtra(EXTRA_DEPARTURE_MS, 0);
        distanceKm   = i.getDoubleExtra(EXTRA_DISTANCE_KM, 0);
        fuelPrice    = i.getDoubleExtra(EXTRA_FUEL_PRICE, 95.0);
        double rating = i.getDoubleExtra(EXTRA_RIDER_RATING, 0);

        // Store rating in a field for UI
        detailRatingValue = rating;
    }
    private double detailRatingValue;

    private void bindViews() {
        detailRiderInitial   = findViewById(R.id.detailRiderInitial);
        detailRiderName      = findViewById(R.id.detailRiderName);
        detailVehicle        = findViewById(R.id.detailVehicle);
        detailRating         = findViewById(R.id.detailRating);
        detailFromName       = findViewById(R.id.detailFromName);
        detailToName         = findViewById(R.id.detailToName);
        detailCost           = findViewById(R.id.detailCost);
        detailSeats          = findViewById(R.id.detailSeats);
        detailTime           = findViewById(R.id.detailTime);
        requestRideBtn       = findViewById(R.id.requestRideBtn);
        requestStatusBanner  = findViewById(R.id.requestStatusBanner);
        requestProgress      = findViewById(R.id.requestProgress);
        requestStatusProgress= findViewById(R.id.requestStatusProgress);
        requestBtnLabel      = findViewById(R.id.requestBtnLabel);
        requestStatusText    = findViewById(R.id.requestStatusText);
        cancelRequestButton  = findViewById(R.id.cancelRequestButton);
        
        // Try to find distance and breakdown views (may not exist in layout)
        try {
            detailDistance = findViewById(R.id.detailDistance);
            detailFuelCostBreakdown = findViewById(R.id.detailFuelCostBreakdown);
        } catch (Exception e) {
            // Views don't exist, which is fine
        }
    }

    private void populateUI() {
        if (riderName != null && !riderName.isEmpty()) {
            detailRiderInitial.setText(String.valueOf(riderName.charAt(0)).toUpperCase());
        }
        detailRiderName.setText(riderName);
        detailVehicle.setText(vehicle != null ? vehicle : "Bike");
        detailRating.setText(detailRatingValue > 0
                ? String.format(Locale.getDefault(), "%.1f ★", detailRatingValue) : "New");
        detailFromName.setText(fromName);
        detailToName.setText(toName);
        detailCost.setText(cost > 0 ? String.format("₹%.0f", cost) : "₹ —");
        detailSeats.setText(String.valueOf(seats));
        if (departureMs > 0) {
            detailTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(departureMs)));
        }
        
        // Display distance and calculate fuel cost breakdown
        if (distanceKm > 0 && detailDistance != null) {
            detailDistance.setText(String.format(Locale.getDefault(), "Distance: %.1f km", distanceKm));
            
            // Calculate fuel cost breakdown
            if (detailFuelCostBreakdown != null) {
                double totalFuelCost = CostCalculator.calculateTotalFuelCost(distanceKm, fuelPrice);
                double costPerPerson = CostCalculator.calculateCostPerPerson(distanceKm, fuelPrice, seats);
                if (costPerPerson > 0) {
                    cost = costPerPerson;
                    detailCost.setText(String.format("₹%.0f", costPerPerson));
                }
                
                String breakdownText = String.format(Locale.getDefault(), 
                    "Fuel: %s | Per person: %s (÷%d)", 
                    CostCalculator.formatCost(totalFuelCost),
                    CostCalculator.formatCost(costPerPerson),
                    seats);
                detailFuelCostBreakdown.setText(breakdownText);
            }
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.detailBackBtn).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        requestRideBtn.setOnClickListener(v -> sendRideRequest());
        if (cancelRequestButton != null) {
            cancelRequestButton.setOnClickListener(v -> cancelRequest());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Send ride request
    // ══════════════════════════════════════════════════════════════════════════

    private void sendRideRequest() {
        setLoading(true);

        RideRequest request = new RideRequest();
        request.setRideId(rideId);
        request.setPassengerId(session.getUid());
        request.setPassengerName(session.getName());
        request.setRiderId(riderId);
        request.setRiderName(riderName);
        request.setPickupName(fromName);
        request.setDropName(toName);
        request.setCostAgreed(cost);
        request.setRequestedAt(com.google.firebase.Timestamp.now());

        repo.createRideRequest(request, new FirestoreRepository.Callback<String>() {
            @Override
            public void onSuccess(String requestId) {
                activeRequestId = requestId;
                setLoading(false);
                requestRideBtn.setVisibility(View.GONE);
                requestStatusBanner.setVisibility(View.VISIBLE);
                listenForRequestUpdates(requestId);
            }
            @Override
            public void onFailure(String error) {
                setLoading(false);
                ErrorHandler.showError(RideDetailActivity.this, 
                    ErrorHandler.ErrorType.FIRESTORE, error);
            }
        });
    }

    private void listenForRequestUpdates(String requestId) {
        requestListener = repo.listenToRideRequest(requestId, new FirestoreRepository.Callback<RideRequest>() {
            @Override
            public void onSuccess(RideRequest req) {
                switch (req.getStatus()) {
                    case RideRequest.STATUS_ACCEPTED:
                        requestStatusProgress.setVisibility(View.GONE);
                        requestStatusText.setText("✅ Rider accepted! Opening chat…");
                        requestStatusText.setTextColor(0xFF4CAF50);
                        if (cancelRequestButton != null) cancelRequestButton.setVisibility(View.GONE);
                        openChat(req);
                        break;
                    case RideRequest.STATUS_REJECTED:
                        requestStatusProgress.setVisibility(View.GONE);
                        requestStatusText.setText("❌ Rider declined this request");
                        requestStatusText.setTextColor(0xFFFF5252);
                        if (cancelRequestButton != null) cancelRequestButton.setVisibility(View.GONE);
                        break;
                    case RideRequest.STATUS_CANCELLED:
                        requestStatusProgress.setVisibility(View.GONE);
                        requestStatusText.setText("Request cancelled");
                        requestStatusText.setTextColor(0xFF9E9E9E);
                        if (cancelRequestButton != null) cancelRequestButton.setVisibility(View.GONE);
                        break;
                    default:
                        requestStatusText.setText("Waiting for rider to accept…");
                        break;
                }
            }
            @Override
            public void onFailure(String error) {
                ErrorHandler.showError(RideDetailActivity.this, 
                    ErrorHandler.ErrorType.FIRESTORE, error);
            }
        });
    }

    private void openChat(RideRequest req) {
        Intent intent = new Intent(this, ChatFragment.class);
        // ChatFragment is a Fragment — launch MainAppActivity navigated to chat tab
        Intent mainIntent = new Intent(this, MainAppActivity.class);
        mainIntent.putExtra("open_chat_id", req.getChatId());
        mainIntent.putExtra("open_chat_name", riderName);
        mainIntent.putExtra("open_chat_request_id", req.getRequestId());
        mainIntent.putExtra("open_chat_ride_id", req.getRideId());
        mainIntent.putExtra("open_chat_rider_id", req.getRiderId());
        mainIntent.putExtra("open_chat_passenger_id", req.getPassengerId());
        mainIntent.putExtra("open_chat_cost_agreed", req.getCostAgreed());
        startActivity(mainIntent);
    }

    private void cancelRequest() {
        if (activeRequestId == null || activeRequestId.isEmpty()) return;

        repo.updateRequestStatus(activeRequestId, RideRequest.STATUS_CANCELLED,
                new FirestoreRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        ErrorHandler.showSuccess(RideDetailActivity.this, "Request cancelled");
                        requestStatusBanner.setVisibility(View.GONE);
                        requestRideBtn.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(String error) {
                        ErrorHandler.showError(RideDetailActivity.this, 
                            ErrorHandler.ErrorType.FIRESTORE, error);
                    }
                });
    }

    private void setLoading(boolean loading) {
        requestProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        requestBtnLabel.setText(loading ? "" : "Request Ride");
        requestRideBtn.setClickable(!loading);
    }
}
