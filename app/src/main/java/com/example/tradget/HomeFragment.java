package com.example.tradget;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.tradget.model.Ride;
import com.example.tradget.ErrorHandler;
import com.example.tradget.CacheManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView fromLocation, toLocation;
    private LinearLayout fromLocationContainer, toLocationContainer;
    private TextView togglePassenger, toggleRider;
    private TextView ridesTitle, searchRideLabel;
    private LinearLayout searchRideBtn;
    private LinearLayout ridesContainer;
    private TextView noRidesLabel;
    private ProgressBar ridesProgress;

    private boolean isPassengerMode = true;
    private boolean isSelectingFrom = true;
    private String fromLocationText = "", toLocationText = "";
    private LatLng fromLatLng = null, toLatLng = null;

    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private FirestoreRepository repo;
    private SessionManager session;
    private CacheManager cache;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        fromLocation          = view.findViewById(R.id.fromLocation);
        toLocation            = view.findViewById(R.id.toLocation);
        fromLocationContainer = view.findViewById(R.id.fromLocationContainer);
        toLocationContainer   = view.findViewById(R.id.toLocationContainer);
        togglePassenger       = view.findViewById(R.id.togglePassenger);
        toggleRider           = view.findViewById(R.id.toggleRider);
        searchRideBtn         = view.findViewById(R.id.searchRideBtn);
        searchRideLabel       = view.findViewById(R.id.searchRideLabel);
        ridesContainer        = view.findViewById(R.id.dynamicRidesContainer);
        ridesTitle            = view.findViewById(R.id.ridesTitle);
        noRidesLabel          = view.findViewById(R.id.noRidesLabel);
        ridesProgress         = view.findViewById(R.id.ridesProgress);

        placesClient        = Places.createClient(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        repo                = FirestoreRepository.getInstance();
        session             = new SessionManager(requireContext());
        cache               = CacheManager.getInstance(requireContext());

        getCurrentLocation();
        fromLocationContainer.setOnClickListener(v -> showLocationSearch(true));
        toLocationContainer.setOnClickListener(v   -> showLocationSearch(false));
        fromLocation.setOnClickListener(v          -> showLocationSearch(true));
        toLocation.setOnClickListener(v            -> showLocationSearch(false));
        togglePassenger.setOnClickListener(v -> setPassengerMode(true));
        toggleRider.setOnClickListener(v     -> setPassengerMode(false));
        searchRideBtn.setOnClickListener(v   -> onSearchOrPublish());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isPassengerMode) {
            // Keep previous search results until the user searches again
        } else {
            loadPendingRequests();
        }
    }

    private void onSearchOrPublish() {
        if (toLatLng == null || toLocationText.isEmpty()) {
            ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.INVALID_INPUT, 
                "Please select a destination"); 
            return;
        }
        if (fromLatLng == null) {
            ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.INVALID_INPUT, 
                "Waiting for your location…"); 
            return;
        }
        if (isPassengerMode) {
            searchRides();
        } else {
            Intent i = new Intent(requireContext(), PublishRideActivity.class);
            i.putExtra(PublishRideActivity.EXTRA_FROM_LAT, fromLatLng.latitude);
            i.putExtra(PublishRideActivity.EXTRA_FROM_LNG, fromLatLng.longitude);
            i.putExtra(PublishRideActivity.EXTRA_FROM_NAME, fromLocationText);
            i.putExtra(PublishRideActivity.EXTRA_TO_LAT, toLatLng.latitude);
            i.putExtra(PublishRideActivity.EXTRA_TO_LNG, toLatLng.longitude);
            i.putExtra(PublishRideActivity.EXTRA_TO_NAME, toLocationText);
            startActivity(i);
        }
    }

    private void searchRides() {
        if (ridesProgress != null) ridesProgress.setVisibility(View.VISIBLE);
        if (ridesContainer != null) ridesContainer.removeAllViews();
        if (noRidesLabel != null) noRidesLabel.setVisibility(View.GONE);

        // Check cache first for the same search parameters
        String cacheKey = "search_" + fromLocationText + "_" + toLocationText;
        List<Ride> cachedRides = cache.get(cacheKey, List.class);
        if (cachedRides != null) {
            if (ridesProgress != null) ridesProgress.setVisibility(View.GONE);
            if (cachedRides.isEmpty()) {
                if (noRidesLabel != null) noRidesLabel.setVisibility(View.VISIBLE);
                return;
            }
            for (Ride ride : cachedRides) addRideCard(ride);
            return;
        }

        repo.searchRides(fromLocationText, toLocationText, new FirestoreRepository.Callback<List<Ride>>() {
            @Override public void onSuccess(List<Ride> rides) {
                // Cache the results
                cache.put(cacheKey, rides, List.class);
                
                if (ridesProgress != null) ridesProgress.setVisibility(View.GONE);
                if (rides.isEmpty()) {
                    if (noRidesLabel != null) noRidesLabel.setVisibility(View.VISIBLE);
                    return;
                }
                for (Ride ride : rides) addRideCard(ride);
            }
            @Override public void onFailure(String error) {
                if (ridesProgress != null) ridesProgress.setVisibility(View.GONE);
                ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.FIRESTORE, error);
            }
        });
    }

    private void addRideCard(Ride ride) {
        if (ridesContainer == null) return;
        LinearLayout card = new LinearLayout(requireContext());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 32);
        card.setLayoutParams(cp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(40, 32, 40, 32);
        card.setElevation(8f);

        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout infoCol = new LinearLayout(requireContext());
        infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        infoCol.setOrientation(LinearLayout.VERTICAL);

        TextView tv1 = new TextView(requireContext());
        tv1.setText(ride.getRiderName() + " • " + ride.getVehicleType());
        tv1.setTextSize(16); tv1.setTextColor(0xFF212121);
        tv1.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tv2 = new TextView(requireContext());
        tv2.setText(ride.getSeatsAvailable() + " seats • " + ride.getRatingDisplay());
        tv2.setTextSize(13); tv2.setTextColor(0xFF757575); tv2.setPadding(0, 4, 0, 0);

        infoCol.addView(tv1); infoCol.addView(tv2);

        LinearLayout actionRow = new LinearLayout(requireContext());
        actionRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView priceView = new TextView(requireContext());
        priceView.setText(ride.getCostDisplay());
        priceView.setTextSize(20); priceView.setTextColor(0xFF00BCD4);
        priceView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView favoriteBtn = new TextView(requireContext());
        favoriteBtn.setText("♡");
        favoriteBtn.setTextSize(24); favoriteBtn.setTextColor(0xFFFF5252);
        favoriteBtn.setPadding(16, 0, 0, 0);
        favoriteBtn.setClickable(true); favoriteBtn.setFocusable(true);
        
        final String passengerId = session.getUid();
        final String riderId = ride.getRiderId();
        
        // Check if already favorited
        repo.isFavorite(passengerId, riderId, new FirestoreRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFav) {
                if (isFav) favoriteBtn.setText("♥");
            }
            @Override
            public void onFailure(String error) {}
        });
        
        favoriteBtn.setOnClickListener(v -> {
            if ("♡".equals(favoriteBtn.getText().toString())) {
                // Add favorite
                repo.addFavorite(passengerId, riderId, new FirestoreRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        favoriteBtn.setText("♥");
                    }
                    @Override
                    public void onFailure(String error) {
                        ErrorHandler.showError(getContext(), 
                            ErrorHandler.ErrorType.FIRESTORE, error);
                    }
                });
            } else {
                // Remove favorite
                repo.removeFavorite(passengerId, riderId, new FirestoreRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        favoriteBtn.setText("♡");
                    }
                    @Override
                    public void onFailure(String error) {
                        ErrorHandler.showError(getContext(), 
                            ErrorHandler.ErrorType.FIRESTORE, error);
                    }
                });
            }
        });

        actionRow.addView(priceView); actionRow.addView(favoriteBtn);

        topRow.addView(infoCol); topRow.addView(actionRow);

        LinearLayout joinBtn = new LinearLayout(requireContext());
        LinearLayout.LayoutParams jp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 108);
        jp.topMargin = 24;
        joinBtn.setLayoutParams(jp);
        joinBtn.setGravity(Gravity.CENTER);
        joinBtn.setBackgroundResource(R.drawable.bg_button_join);
        joinBtn.setClickable(true); joinBtn.setFocusable(true);

        TextView joinLabel = new TextView(requireContext());
        joinLabel.setText("Join Ride"); joinLabel.setTextSize(14);
        joinLabel.setTextColor(0xFFFFFFFF);
        joinLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        joinBtn.addView(joinLabel);
        joinBtn.setOnClickListener(v -> openRideDetail(ride));

        card.addView(topRow); card.addView(joinBtn);
        ridesContainer.addView(card);
    }

    private void openRideDetail(Ride ride) {
        Intent i = new Intent(requireContext(), RideDetailActivity.class);
        i.putExtra(RideDetailActivity.EXTRA_RIDE_ID,      ride.getRideId());
        i.putExtra(RideDetailActivity.EXTRA_RIDER_ID,     ride.getRiderId());
        i.putExtra(RideDetailActivity.EXTRA_RIDER_NAME,   ride.getRiderName());
        i.putExtra(RideDetailActivity.EXTRA_RIDER_RATING, ride.getRiderRating());
        i.putExtra(RideDetailActivity.EXTRA_VEHICLE,      ride.getVehicleType());
        i.putExtra(RideDetailActivity.EXTRA_FROM_NAME,    ride.getFromName());
        i.putExtra(RideDetailActivity.EXTRA_TO_NAME,      ride.getToName());
        i.putExtra(RideDetailActivity.EXTRA_FROM_LAT,     ride.getFromLat());
        i.putExtra(RideDetailActivity.EXTRA_FROM_LNG,     ride.getFromLng());
        i.putExtra(RideDetailActivity.EXTRA_TO_LAT,       ride.getToLat());
        i.putExtra(RideDetailActivity.EXTRA_TO_LNG,       ride.getToLng());
        i.putExtra(RideDetailActivity.EXTRA_COST,         ride.getCostPerPerson());
        i.putExtra(RideDetailActivity.EXTRA_SEATS,        ride.getSeatsAvailable());
        i.putExtra(RideDetailActivity.EXTRA_DISTANCE_KM,  ride.getDistanceKm());
        i.putExtra(RideDetailActivity.EXTRA_FUEL_PRICE,   ride.getFuelPrice());
        if (ride.getDepartureTime() != null)
            i.putExtra(RideDetailActivity.EXTRA_DEPARTURE_MS, ride.getDepartureTime().toDate().getTime());
        startActivity(i);
    }

    private void showLocationSearch(boolean isFrom) {
        isSelectingFrom = isFrom;
        View sv = LayoutInflater.from(getContext()).inflate(R.layout.location_search_dialog, null);
        EditText si       = sv.findViewById(R.id.searchInput);
        ImageView bb      = sv.findViewById(R.id.backButton);
        TextView st       = sv.findViewById(R.id.searchTitle);
        LinearLayout rc   = sv.findViewById(R.id.resultsContainer);
        st.setText(isFrom ? "Select Pickup Location" : "Select Destination");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(sv);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        bb.setOnClickListener(v -> dialog.dismiss());
        si.requestFocus();
        ((InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE))
                .showSoftInput(si, InputMethodManager.SHOW_IMPLICIT);

        si.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { searchHandler.removeCallbacks(searchRunnable); }
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.length() < 2) { rc.removeAllViews(); return; }
                searchRunnable = () -> searchPlaces(q, rc, dialog);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void searchPlaces(String query, LinearLayout rc, AlertDialog dialog) {
        placesClient.findAutocompletePredictions(
                FindAutocompletePredictionsRequest.builder().setQuery(query).setCountry("IN").build()
        ).addOnSuccessListener(response -> {
            rc.removeAllViews();
            for (AutocompletePrediction p : response.getAutocompletePredictions()) {
                LinearLayout item = new LinearLayout(getContext());
                item.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                item.setOrientation(LinearLayout.VERTICAL);
                item.setPadding(32, 32, 32, 32);
                item.setBackgroundResource(android.R.drawable.list_selector_background);
                item.setClickable(true);

                TextView t1 = new TextView(getContext());
                t1.setText(p.getPrimaryText(null).toString());
                t1.setTextSize(15); t1.setTextColor(0xFF212121);

                TextView t2 = new TextView(getContext());
                t2.setText(p.getSecondaryText(null).toString());
                t2.setTextSize(12); t2.setTextColor(0xFF757575); t2.setPadding(0, 4, 0, 0);

                item.addView(t1); item.addView(t2);

                View div = new View(getContext());
                div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                div.setBackgroundColor(0xFFE0E0E0);

                item.setOnClickListener(v -> fetchPlaceDetails(p.getPlaceId(), dialog));
                rc.addView(item); rc.addView(div);
            }
        }).addOnFailureListener(e ->
                ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.NETWORK, e.getMessage()));
    }

    private void fetchPlaceDetails(String placeId, AlertDialog dialog) {
        placesClient.fetchPlace(
                FetchPlaceRequest.builder(placeId, Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)).build()
        ).addOnSuccessListener(resp -> {
            Place place = resp.getPlace();
            String name = place.getName() != null ? place.getName() : "";
            LatLng ll   = place.getLatLng();
            if (isSelectingFrom) { fromLocationText = name; fromLatLng = ll; fromLocation.setText(name); }
            else                 { toLocationText   = name; toLatLng   = ll; toLocation.setText(name);   }
            dialog.dismiss();
        }).addOnFailureListener(e -> ErrorHandler.showError(getContext(), 
            ErrorHandler.ErrorType.NETWORK, e.getMessage()));
    }

    private void setPassengerMode(boolean isPassenger) {
        isPassengerMode = isPassenger;
        if (isPassenger) {
            togglePassenger.setBackgroundResource(R.drawable.bg_toggle_selected);
            togglePassenger.setTextColor(0xFFFFFFFF);
            toggleRider.setBackgroundResource(android.R.color.transparent);
            toggleRider.setTextColor(0xFF757575);
            if (searchRideLabel != null) searchRideLabel.setText("Search Ride");
            if (ridesTitle != null) ridesTitle.setText("Available Rides");
            if (noRidesLabel != null) noRidesLabel.setText(
                    "No rides found for this route.\nTry a different destination or check back later.");
            if (ridesContainer != null) ridesContainer.removeAllViews();
        } else {
            toggleRider.setBackgroundResource(R.drawable.bg_toggle_selected);
            toggleRider.setTextColor(0xFFFFFFFF);
            togglePassenger.setBackgroundResource(android.R.color.transparent);
            togglePassenger.setTextColor(0xFF757575);
            if (searchRideLabel != null) searchRideLabel.setText("Publish Ride");
            if (ridesTitle != null) ridesTitle.setText("Pending Requests");
            if (noRidesLabel != null) noRidesLabel.setText("No pending requests yet.");
            loadPendingRequests();
        }
    }

    private void loadPendingRequests() {
        if (ridesProgress != null) ridesProgress.setVisibility(View.VISIBLE);
        if (ridesContainer != null) ridesContainer.removeAllViews();
        if (noRidesLabel != null) noRidesLabel.setVisibility(View.GONE);

        repo.getPendingRequestsForRider(session.getUid(), new FirestoreRepository.Callback<List<com.example.tradget.model.RideRequest>>() {
            @Override
            public void onSuccess(List<com.example.tradget.model.RideRequest> requests) {
                if (ridesProgress != null) ridesProgress.setVisibility(View.GONE);
                if (requests.isEmpty()) {
                    if (noRidesLabel != null) noRidesLabel.setVisibility(View.VISIBLE);
                    return;
                }
                for (com.example.tradget.model.RideRequest req : requests) {
                    addRequestCard(req);
                }
            }

            @Override
            public void onFailure(String error) {
                if (ridesProgress != null) ridesProgress.setVisibility(View.GONE);
                ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.FIRESTORE, error);
            }
        });
    }

    private void addRequestCard(com.example.tradget.model.RideRequest req) {
        if (ridesContainer == null) return;

        LinearLayout card = new LinearLayout(requireContext());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 32);
        card.setLayoutParams(cp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(40, 32, 40, 32);
        card.setElevation(8f);

        TextView title = new TextView(requireContext());
        title.setText((req.getPassengerName() != null ? req.getPassengerName() : "Passenger")
                + " • ₹" + String.format(Locale.getDefault(), "%.0f", req.getCostAgreed()));
        title.setTextSize(16);
        title.setTextColor(0xFF212121);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView route = new TextView(requireContext());
        route.setText(req.getPickupName() + " → " + req.getDropName());
        route.setTextSize(13);
        route.setTextColor(0xFF757575);
        route.setPadding(0, 6, 0, 0);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ap.topMargin = 20;
        actions.setLayoutParams(ap);

        TextView acceptBtn = new TextView(requireContext());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, 96, 1f);
        btnLp.setMarginEnd(12);
        acceptBtn.setLayoutParams(btnLp);
        acceptBtn.setText("Accept");
        acceptBtn.setGravity(Gravity.CENTER);
        acceptBtn.setTextColor(0xFFFFFFFF);
        acceptBtn.setTextSize(14);
        acceptBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        acceptBtn.setBackgroundResource(R.drawable.bg_button_cyan);
        acceptBtn.setClickable(true);
        acceptBtn.setFocusable(true);

        TextView rejectBtn = new TextView(requireContext());
        LinearLayout.LayoutParams rejectLp = new LinearLayout.LayoutParams(0, 96, 1f);
        rejectBtn.setLayoutParams(rejectLp);
        rejectBtn.setText("Reject");
        rejectBtn.setGravity(Gravity.CENTER);
        rejectBtn.setTextColor(0xFF757575);
        rejectBtn.setTextSize(14);
        rejectBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        rejectBtn.setBackgroundResource(R.drawable.bg_input_field);
        rejectBtn.setClickable(true);
        rejectBtn.setFocusable(true);

        actions.addView(acceptBtn);
        actions.addView(rejectBtn);

        acceptBtn.setOnClickListener(v -> handleRequestDecision(req, true));
        rejectBtn.setOnClickListener(v -> handleRequestDecision(req, false));

        card.addView(title);
        card.addView(route);
        card.addView(actions);
        ridesContainer.addView(card);
    }

    private void handleRequestDecision(com.example.tradget.model.RideRequest req, boolean accept) {
        if (!accept) {
            repo.updateRequestStatus(req.getRequestId(),
                    com.example.tradget.model.RideRequest.STATUS_REJECTED,
                    new FirestoreRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            ErrorHandler.showSuccess(getContext(), "Request rejected");
                            loadPendingRequests();
                        }

                        @Override
                        public void onFailure(String error) {
                            ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.FIRESTORE, error);
                        }
                    });
            return;
        }

        // Accept flow: validate seat availability first
        repo.getRideById(req.getRideId(), new FirestoreRepository.Callback<com.example.tradget.model.Ride>() {
            @Override
            public void onSuccess(com.example.tradget.model.Ride ride) {
                if (ride.getSeatsAvailable() <= 0) {
                    ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.CONFLICT, "No seats left for this ride");
                    loadPendingRequests();
                    return;
                }

                repo.updateRequestStatus(req.getRequestId(),
                        com.example.tradget.model.RideRequest.STATUS_ACCEPTED,
                        new FirestoreRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                updateSeatsAfterAccept(req, ride);
                            }

                            @Override
                            public void onFailure(String error) {
                                ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.FIRESTORE, error);
                            }
                        });
            }

            @Override
            public void onFailure(String error) {
                ErrorHandler.showError(getContext(), ErrorHandler.ErrorType.FIRESTORE, error);
                loadPendingRequests();
            }
        });
    }

    private void updateSeatsAfterAccept(com.example.tradget.model.RideRequest req,
                                        com.example.tradget.model.Ride ride) {
        int updatedSeats = Math.max(0, ride.getSeatsAvailable() - 1);
        repo.updateRideSeats(ride.getRideId(), updatedSeats, new FirestoreRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (updatedSeats <= 0) {
                    repo.updateRideStatus(ride.getRideId(), com.example.tradget.model.Ride.STATUS_IN_PROGRESS,
                            new FirestoreRepository.SimpleCallback() {
                                @Override
                                public void onSuccess() {
                                    ErrorHandler.showSuccess(getContext(), "Ride is now in progress");
                                    loadPendingRequests();
                                }

                                @Override
                                public void onFailure(String error) {
                                    ErrorHandler.showError(getContext(), 
                                        ErrorHandler.ErrorType.FIRESTORE, error);
                                    loadPendingRequests();
                                }
                            });
                } else {
                    ErrorHandler.showSuccess(getContext(), "Request accepted");
                    loadPendingRequests();
                }
            }

            @Override
            public void onFailure(String error) {
                ErrorHandler.showError(getContext(), 
                    ErrorHandler.ErrorType.FIRESTORE, error);
                loadPendingRequests();
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100); return;
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) { fromLatLng = new LatLng(loc.getLatitude(), loc.getLongitude()); resolveAddress(loc.getLatitude(), loc.getLongitude()); }
                    else fromLocation.setText("Location not detected");
                });
    }

    private void resolveAddress(double lat, double lng) {
        try {
            List<Address> addresses = new Geocoder(getContext(), Locale.getDefault()).getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                String name = (a.getSubLocality() != null ? a.getSubLocality() + ", " : "") + (a.getLocality() != null ? a.getLocality() : "Current Location");
                fromLocationText = name; fromLocation.setText(name);
            } else { fromLocationText = "Current Location"; fromLocation.setText("Current Location"); }
        } catch (IOException e) { fromLocationText = "Current Location"; fromLocation.setText("Current Location"); }
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(rc, perms, grants);
        if (rc == 100 && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) getCurrentLocation();
    }
}