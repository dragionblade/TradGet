package com.example.tradget;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.tradget.model.Ride;
import com.example.tradget.model.RideRequest;
import com.example.tradget.CacheManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Profile screen — loads real data from Firestore via {@link FirestoreRepository}
 * and binds it to the UI. Also handles logout.
 */
public class ProfileFragment extends Fragment {

    private LinearLayout menuRideHistory, menuPaymentMethods, menuEmergency,
                         menuSettings, menuHelp, logoutButton;
    private TextView tvName, tvPhone, tvRidesTaken, tvSaved, tvRating;

    private SessionManager     session;
    private FirestoreRepository repo;
    private CacheManager        cache;

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        session = new SessionManager(requireContext());
        repo    = FirestoreRepository.getInstance();
        cache   = CacheManager.getInstance(requireContext());

        bindViews(view);
        populateFromCache();
        refreshFromFirestore();
        setupClickListeners();

        return view;
    }

    private void bindViews(View view) {
        menuRideHistory    = view.findViewById(R.id.menuRideHistory);
        menuPaymentMethods = view.findViewById(R.id.menuPaymentMethods);
        menuEmergency      = view.findViewById(R.id.menuEmergency);
        menuSettings       = view.findViewById(R.id.menuSettings);
        menuHelp           = view.findViewById(R.id.menuHelp);
        logoutButton       = view.findViewById(R.id.logoutButton);

        tvName       = view.findViewById(R.id.profileName);
        tvPhone      = view.findViewById(R.id.profileContact);
        tvRidesTaken = view.findViewById(R.id.profileRidesTaken);
        tvSaved      = view.findViewById(R.id.profileSaved);
        tvRating     = view.findViewById(R.id.profileRating);
    }

    /** Immediately shows cached data so the screen never looks empty. */
    private void populateFromCache() {
        if (tvName  != null) tvName.setText(session.getName());
        if (tvPhone != null) tvPhone.setText(session.getPhone().isEmpty()
                ? session.getEmail() : session.getPhone());
        if (tvRidesTaken != null) tvRidesTaken.setText(String.valueOf(session.getRidesDone()));
        if (tvSaved != null) tvSaved.setText(
                String.format("₹%.0f", session.getTotalSaved()));
        if (tvRating != null) tvRating.setText(
                session.getRating() > 0
                        ? String.format("%.1f", session.getRating()) : "New");
    }

    /** Fetches fresh data from Firestore and updates the UI + cache. */
    private void refreshFromFirestore() {
        repo.getUserProfile(session.getUid(), new FirestoreRepository.Callback<com.example.tradget.model.User>() {
            @Override
            public void onSuccess(com.example.tradget.model.User user) {
                // Cache to both SessionManager and CacheManager
                session.saveSession(user.getUid(), user.getName(), user.getEmail());
                session.saveProfile(user.getPhone(), user.getRating(),
                        user.getRidesCompleted(), user.getTotalSaved());
                cache.put("user_profile_" + session.getUid(), user, com.example.tradget.model.User.class);

                if (tvName  != null) tvName.setText(user.getName());
                if (tvPhone != null) tvPhone.setText(
                        user.getPhone() != null && !user.getPhone().isEmpty()
                                ? user.getPhone() : user.getEmail());
                if (tvRidesTaken != null) tvRidesTaken.setText(
                        String.valueOf(user.getRidesCompleted()));
                if (tvSaved != null) tvSaved.setText(
                        String.format("₹%.0f", user.getTotalSaved()));
                if (tvRating != null) tvRating.setText(user.getRatingDisplay());
            }
            @Override
            public void onFailure(String error) {
                // Keep cached values — no toast needed, silent fallback
            }
        });
    }

    private void setupClickListeners() {
        menuRideHistory.setOnClickListener(v ->
                ErrorHandler.showSuccess(getContext(), "Switch to History tab"));
        menuPaymentMethods.setOnClickListener(v ->
                ErrorHandler.showSuccess(getContext(), "Payment Methods — coming soon"));
        menuEmergency.setOnClickListener(v ->
                ErrorHandler.showSuccess(getContext(), "Emergency Contacts — coming soon"));
        menuSettings.setOnClickListener(v ->
                ErrorHandler.showSuccess(getContext(), "Settings — coming soon"));
        menuHelp.setOnClickListener(v ->
                ErrorHandler.showSuccess(getContext(), "Help & Support — coming soon"));

        logoutButton.setOnClickListener(v -> logout());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        session.clearSession();
        startActivity(new Intent(requireContext(), AuthActivity.class));
        requireActivity().finish();
    }

    
}