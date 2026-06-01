package com.example.tradget;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradget.model.Ride;
import com.example.tradget.util.CostCalculator;
import com.example.tradget.util.DistanceCalculator;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Allows a rider to publish a new ride offer to Firestore.
 * Receives from/to location data from {@link HomeFragment} via Intent extras.
 */
public class PublishRideActivity extends AppCompatActivity {

    // ── Intent extras (passed from HomeFragment) ───────────────────────────────
    public static final String EXTRA_FROM_LAT  = "pub_from_lat";
    public static final String EXTRA_FROM_LNG  = "pub_from_lng";
    public static final String EXTRA_FROM_NAME = "pub_from_name";
    public static final String EXTRA_TO_LAT    = "pub_to_lat";
    public static final String EXTRA_TO_LNG    = "pub_to_lng";
    public static final String EXTRA_TO_NAME   = "pub_to_name";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView     publishFromName, publishToName, departureTimeLabel;
    private TextView     vehicleBike, vehicleScooty, vehicleCar;
    private TextView     seatsCount, seatsDecrease, seatsIncrease;
    private EditText     publishFuelPrice;
    private LinearLayout publishRideBtn, departureTimeBtn;
    private ProgressBar  publishProgress;
    private TextView     publishBtnLabel;
    private TextView     distanceDisplay, costDisplay;

    // ── State ──────────────────────────────────────────────────────────────────
    private double fromLat, fromLng, toLat, toLng;
    private String fromName = "", toName = "";
    private String selectedVehicle = "Bike";
    private int    seatsAvailable  = 2;
    private Calendar departureCalendar;
    private double calculatedDistanceKm = 0;
    private double calculatedCostPerPerson = 0;

    // ── Services ───────────────────────────────────────────────────────────────
    private SessionManager     session;
    private FirestoreRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_ride);
        overridePendingTransition(R.anim.slide_up, R.anim.fade_in);

        session = new SessionManager(this);
        repo    = FirestoreRepository.getInstance();

        parseExtras();
        bindViews();
        setupClickListeners();
    }

    private void parseExtras() {
        Intent i = getIntent();
        fromLat  = i.getDoubleExtra(EXTRA_FROM_LAT,  0);
        fromLng  = i.getDoubleExtra(EXTRA_FROM_LNG,  0);
        toLat    = i.getDoubleExtra(EXTRA_TO_LAT,    0);
        toLng    = i.getDoubleExtra(EXTRA_TO_LNG,    0);
        fromName = i.getStringExtra(EXTRA_FROM_NAME) != null ? i.getStringExtra(EXTRA_FROM_NAME) : "";
        toName   = i.getStringExtra(EXTRA_TO_NAME)   != null ? i.getStringExtra(EXTRA_TO_NAME)   : "";
    }

    private void bindViews() {
        publishFromName    = findViewById(R.id.publishFromName);
        publishToName      = findViewById(R.id.publishToName);
        departureTimeLabel = findViewById(R.id.departureTimeLabel);
        vehicleBike        = findViewById(R.id.vehicleBike);
        vehicleScooty      = findViewById(R.id.vehicleScooty);
        vehicleCar         = findViewById(R.id.vehicleCar);
        seatsCount         = findViewById(R.id.seatsCount);
        seatsDecrease      = findViewById(R.id.seatsDecrease);
        seatsIncrease      = findViewById(R.id.seatsIncrease);
        publishFuelPrice   = findViewById(R.id.publishFuelPrice);
        publishRideBtn     = findViewById(R.id.publishRideBtn);
        departureTimeBtn   = findViewById(R.id.departureTimeBtn);
        publishProgress    = findViewById(R.id.publishProgress);
        publishBtnLabel    = findViewById(R.id.publishBtnLabel);
        
        // Try to find distance and cost displays (may not exist in layout)
        try {
            distanceDisplay = findViewById(R.id.distanceDisplay);
            costDisplay = findViewById(R.id.costDisplay);
        } catch (Exception e) {
            // Views don't exist, which is fine
        }

        publishFromName.setText(fromName);
        publishToName.setText(toName);
        
        // Calculate distance if coordinates are available
        if (fromLat != 0 && fromLng != 0 && toLat != 0 && toLng != 0) {
            calculateDistance();
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.publishBackButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_up);
        });

        departureTimeBtn.setOnClickListener(v -> showDateTimePicker());

        vehicleBike.setOnClickListener(v    -> selectVehicle("Bike"));
        vehicleScooty.setOnClickListener(v  -> selectVehicle("Scooty"));
        vehicleCar.setOnClickListener(v     -> selectVehicle("Car"));

        seatsDecrease.setOnClickListener(v -> {
            if (seatsAvailable > 1) {
                seatsAvailable--;
                seatsCount.setText(String.valueOf(seatsAvailable));
                updateCostDisplay();
            }
        });
        seatsIncrease.setOnClickListener(v -> {
            if (seatsAvailable < 6) {
                seatsAvailable++;
                seatsCount.setText(String.valueOf(seatsAvailable));
                updateCostDisplay();
            }
        });
        
        // Recalculate cost when fuel price changes
        publishFuelPrice.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { updateCostDisplay(); }
        });

        publishRideBtn.setOnClickListener(v -> publishRide());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Date / Time Picker
    // ══════════════════════════════════════════════════════════════════════════

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (tv, hour, minute) -> {
                departureCalendar = Calendar.getInstance();
                departureCalendar.set(year, month, day, hour, minute, 0);
                departureTimeLabel.setText(
                        String.format(Locale.getDefault(),
                                "%02d/%02d/%d at %02d:%02d",
                                day, month + 1, year, hour, minute));
                departureTimeLabel.setTextColor(0xFF212121);
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Distance and Cost Calculation
    // ══════════════════════════════════════════════════════════════════════════

    private void calculateDistance() {
        String apiKey = BuildConfig.MAPS_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            applyDistanceFallback();
            return;
        }

        DistanceCalculator.calculateDistance(this,
                fromLat, fromLng, toLat, toLng, apiKey,
                new DistanceCalculator.DistanceCallback() {
                    @Override
                    public void onSuccess(double distanceKm, String durationText) {
                        calculatedDistanceKm = distanceKm;
                        updateCostDisplay();

                        if (distanceDisplay != null) {
                            distanceDisplay.setText(String.format(Locale.getDefault(),
                                    "Distance: %.1f km", distanceKm));
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        applyDistanceFallback();
                    }
                });
    }

    private void applyDistanceFallback() {
        calculatedDistanceKm = DistanceCalculator.computeHaversineKm(
                fromLat, fromLng, toLat, toLng);
        updateCostDisplay();

        if (distanceDisplay != null) {
            distanceDisplay.setText(String.format(Locale.getDefault(),
                    "Distance: %.1f km", calculatedDistanceKm));
        }
    }

    private void updateCostDisplay() {
        if (calculatedDistanceKm <= 0 || costDisplay == null) {
            return;
        }

        double fuelPrice;
        try {
            fuelPrice = Double.parseDouble(publishFuelPrice.getText().toString().trim());
        } catch (NumberFormatException e) {
            fuelPrice = 95.0;
        }

        calculatedCostPerPerson = CostCalculator.calculateCostPerPerson(
            calculatedDistanceKm, fuelPrice, seatsAvailable);
        
        costDisplay.setText(String.format(Locale.getDefault(),
            "Cost per person: %s", CostCalculator.formatCost(calculatedCostPerPerson)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Vehicle selector
    // ══════════════════════════════════════════════════════════════════════════

    private void selectVehicle(String type) {
        selectedVehicle = type;
        resetVehicleButtons();
        switch (type) {
            case "Bike":
                vehicleBike.setBackgroundResource(R.drawable.bg_toggle_selected);
                vehicleBike.setTextColor(0xFFFFFFFF);
                break;
            case "Scooty":
                vehicleScooty.setBackgroundResource(R.drawable.bg_toggle_selected);
                vehicleScooty.setTextColor(0xFFFFFFFF);
                break;
            case "Car":
                vehicleCar.setBackgroundResource(R.drawable.bg_toggle_selected);
                vehicleCar.setTextColor(0xFFFFFFFF);
                break;
        }
    }

    private void resetVehicleButtons() {
        for (TextView btn : new TextView[]{vehicleBike, vehicleScooty, vehicleCar}) {
            btn.setBackgroundResource(R.drawable.bg_input_field);
            btn.setTextColor(0xFF757575);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Publish ride to Firestore
    // ══════════════════════════════════════════════════════════════════════════

    private void publishRide() {
        if (departureCalendar == null) {
            Toast.makeText(this, "Please select departure time", Toast.LENGTH_SHORT).show();
            return;
        }

        double fuelPrice;
        try {
            fuelPrice = Double.parseDouble(publishFuelPrice.getText().toString().trim());
        } catch (NumberFormatException e) {
            fuelPrice = 95.0;
        }

        // Use calculated cost per person, or calculate now if not done
        double costPerPerson = calculatedCostPerPerson;
        if (costPerPerson == 0 && calculatedDistanceKm > 0) {
            costPerPerson = CostCalculator.calculateCostPerPerson(
                calculatedDistanceKm, fuelPrice, seatsAvailable);
        }

        Ride ride = new Ride();
        ride.setRiderId(session.getUid());
        ride.setRiderName(session.getName());
        ride.setRiderRating(session.getRating());
        ride.setVehicleType(selectedVehicle);
        ride.setFromName(fromName);
        ride.setFromLat(fromLat);
        ride.setFromLng(fromLng);
        ride.setToName(toName);
        ride.setToLat(toLat);
        ride.setToLng(toLng);
        ride.setDepartureTime(new Timestamp(new Date(departureCalendar.getTimeInMillis())));
        ride.setSeatsAvailable(seatsAvailable);
        ride.setFuelPrice(fuelPrice);
        ride.setDistanceKm(calculatedDistanceKm);  // Store calculated distance
        ride.setCostPerPerson(costPerPerson);       // Store calculated cost
        ride.setStatus(Ride.STATUS_ACTIVE);
        ride.setCreatedAt(new Timestamp(new Date()));

        setLoading(true);
        repo.publishRide(ride, new FirestoreRepository.Callback<String>() {
            @Override
            public void onSuccess(String rideId) {
                setLoading(false);
                Toast.makeText(PublishRideActivity.this,
                        "Ride published! Passengers can now find you.", Toast.LENGTH_LONG).show();

                // Navigate to MapActivity to show the route
                Intent mapIntent = new Intent(PublishRideActivity.this, MapActivity.class);
                mapIntent.putExtra(MapActivity.EXTRA_FROM_LAT,  fromLat);
                mapIntent.putExtra(MapActivity.EXTRA_FROM_LNG,  fromLng);
                mapIntent.putExtra(MapActivity.EXTRA_FROM_NAME, fromName);
                mapIntent.putExtra(MapActivity.EXTRA_TO_LAT,    toLat);
                mapIntent.putExtra(MapActivity.EXTRA_TO_LNG,    toLng);
                mapIntent.putExtra(MapActivity.EXTRA_TO_NAME,   toName);
                mapIntent.putExtra(MapActivity.EXTRA_MODE,      "Rider");
                startActivity(mapIntent);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.slide_up);
            }
            @Override
            public void onFailure(String error) {
                setLoading(false);
                Toast.makeText(PublishRideActivity.this,
                        "Failed to publish ride: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        publishProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        publishBtnLabel.setText(loading ? "" : "Publish Ride");
        publishRideBtn.setClickable(!loading);
    }
}
