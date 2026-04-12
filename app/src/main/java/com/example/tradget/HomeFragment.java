package com.example.tradget;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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

    TextView fromLocation, toLocation;
    LinearLayout fromLocationContainer, toLocationContainer;
    TextView togglePassenger, toggleRider;
    LinearLayout searchRideBtn;
    
    boolean isPassengerMode = true;
    String fromLocationText = "";
    String toLocationText = "";
    boolean isSelectingFrom = true;
    
    FusedLocationProviderClient fusedLocationClient;
    PlacesClient placesClient;
    Handler searchHandler = new Handler(Looper.getMainLooper());
    Runnable searchRunnable;

    public HomeFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        fromLocation = view.findViewById(R.id.fromLocation);
        toLocation = view.findViewById(R.id.toLocation);
        fromLocationContainer = view.findViewById(R.id.fromLocationContainer);
        toLocationContainer = view.findViewById(R.id.toLocationContainer);
        togglePassenger = view.findViewById(R.id.togglePassenger);
        toggleRider = view.findViewById(R.id.toggleRider);
        searchRideBtn = view.findViewById(R.id.searchRideBtn);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyB2Sj6qedyMxJFm851wbc7AjlKgCrz1lkk");
        }
        placesClient = Places.createClient(requireContext());

        getCurrentLocation();

        fromLocationContainer.setOnClickListener(v -> showLocationSearch(true));
        toLocationContainer.setOnClickListener(v -> showLocationSearch(false));
        fromLocation.setOnClickListener(v -> showLocationSearch(true));
        toLocation.setOnClickListener(v -> showLocationSearch(false));
        
        togglePassenger.setOnClickListener(v -> setPassengerMode(true));
        toggleRider.setOnClickListener(v -> setPassengerMode(false));
        
        searchRideBtn.setOnClickListener(v -> {
            if (toLocationText.isEmpty()) {
                Toast.makeText(getContext(), "Please select destination", Toast.LENGTH_SHORT).show();
                return;
            }
            String mode = isPassengerMode ? "Passenger" : "Rider";
            Toast.makeText(getContext(), "Searching rides...\nMode: " + mode + "\nFrom: " + fromLocationText + "\nTo: " + toLocationText, Toast.LENGTH_LONG).show();
        });

        return view;
    }
    
    private void showLocationSearch(boolean isFrom) {
        isSelectingFrom = isFrom;
        
        View searchView = LayoutInflater.from(getContext()).inflate(R.layout.location_search_dialog, null);
        
        EditText searchInput = searchView.findViewById(R.id.searchInput);
        ImageView backButton = searchView.findViewById(R.id.backButton);
        TextView searchTitle = searchView.findViewById(R.id.searchTitle);
        LinearLayout resultsContainer = searchView.findViewById(R.id.resultsContainer);
        
        searchTitle.setText(isFrom ? "Select Pickup Location" : "Select Destination");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(searchView);
        AlertDialog dialog = builder.create();
        dialog.show();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        
        backButton.setOnClickListener(v -> dialog.dismiss());
        
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() < 2) {
                    resultsContainer.removeAllViews();
                    return;
                }
                searchRunnable = () -> searchPlaces(query, resultsContainer, dialog);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }
    
    private void searchPlaces(String query, LinearLayout resultsContainer, AlertDialog dialog) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setCountry("IN")
                .build();
        
        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            resultsContainer.removeAllViews();
            
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                String placeId = prediction.getPlaceId();
                String primaryText = prediction.getPrimaryText(null).toString();
                String secondaryText = prediction.getSecondaryText(null).toString();
                
                LinearLayout item = new LinearLayout(getContext());
                item.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                item.setOrientation(LinearLayout.VERTICAL);
                item.setPadding(16, 16, 16, 16);
                item.setBackgroundResource(android.R.drawable.list_selector_background);
                item.setClickable(true);
                
                TextView primary = new TextView(getContext());
                primary.setText(primaryText);
                primary.setTextSize(16);
                primary.setTextColor(0xFF212121);
                
                TextView secondary = new TextView(getContext());
                secondary.setText(secondaryText);
                secondary.setTextSize(13);
                secondary.setTextColor(0xFF757575);
                
                item.addView(primary);
                item.addView(secondary);
                
                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(0xFFE0E0E0);
                
                item.setOnClickListener(v -> {
                    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);
                    FetchPlaceRequest fetchRequest = FetchPlaceRequest.builder(placeId, placeFields).build();
                    
                    placesClient.fetchPlace(fetchRequest).addOnSuccessListener(fetchResponse -> {
                        Place place = fetchResponse.getPlace();
                        String placeName = place.getName();
                        
                        if (isSelectingFrom) {
                            fromLocationText = placeName;
                            fromLocation.setText(placeName);
                        } else {
                            toLocationText = placeName;
                            toLocation.setText(placeName);
                        }
                        
                        dialog.dismiss();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
                
                resultsContainer.addView(item);
                resultsContainer.addView(divider);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setPassengerMode(boolean isPassenger) {
        isPassengerMode = isPassenger;
        if (isPassenger) {
            togglePassenger.setBackgroundResource(R.drawable.bg_toggle_selected);
            togglePassenger.setTextColor(getResources().getColor(android.R.color.white));
            toggleRider.setBackgroundResource(android.R.color.transparent);
            toggleRider.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            toggleRider.setBackgroundResource(R.drawable.bg_toggle_selected);
            toggleRider.setTextColor(getResources().getColor(android.R.color.white));
            togglePassenger.setBackgroundResource(android.R.color.transparent);
            togglePassenger.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        fromLocation.setText("Location not detected");
                    }
                });
    }

    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getSubLocality() + ", " + address.getLocality();
                fromLocationText = locationName;
                fromLocation.setText(locationName);
            } else {
                fromLocationText = "Current Location";
                fromLocation.setText("Current Location");
            }
        } catch (IOException e) {
            fromLocationText = "Current Location";
            fromLocation.setText("Current Location");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}