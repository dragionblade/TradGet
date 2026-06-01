package com.example.tradget;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.tradget.model.Ride;
import com.example.tradget.model.RideRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Shows ride history loaded from Firestore — both as passenger and as rider.
 * Tabs: Completed / Cancelled / Scheduled (all backed by real Firestore queries).
 */
public class HistoryFragment extends Fragment {

    private TextView     tabCompleted, tabCancelled, tabScheduled;
    private LinearLayout ridesContainer;

    private SessionManager     session;
    private FirestoreRepository repo;

    public HistoryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        tabCompleted   = view.findViewById(R.id.tabCompleted);
        tabCancelled   = view.findViewById(R.id.tabCancelled);
        tabScheduled   = view.findViewById(R.id.tabScheduled);
        ridesContainer = view.findViewById(R.id.ridesContainer);

        session = new SessionManager(requireContext());
        repo    = FirestoreRepository.getInstance();

        tabCompleted.setOnClickListener(v -> switchTab("completed"));
        tabCancelled.setOnClickListener(v -> switchTab("cancelled"));
        tabScheduled.setOnClickListener(v -> switchTab("scheduled"));

        // Load completed by default
        switchTab("completed");

        return view;
    }

    private void switchTab(String tab) {
        resetTabs();
        switch (tab) {
            case "completed":
                tabCompleted.setTextColor(0xFF00BCD4);
                tabCompleted.setBackgroundColor(0xFFE3F2FD);
                loadCompletedRides();
                break;
            case "cancelled":
                tabCancelled.setTextColor(0xFF00BCD4);
                tabCancelled.setBackgroundColor(0xFFE3F2FD);
                loadCancelledRides();
                break;
            case "scheduled":
                tabScheduled.setTextColor(0xFF00BCD4);
                tabScheduled.setBackgroundColor(0xFFE3F2FD);
                loadScheduledRides();
                break;
        }
    }

    private void resetTabs() {
        for (TextView t : new TextView[]{tabCompleted, tabCancelled, tabScheduled}) {
            t.setTextColor(0xFF757575);
            t.setBackgroundColor(0xFFFFFFFF);
        }
    }

    private void loadCompletedRides() {
        ridesContainer.removeAllViews();
        String uid = session.getUid();

        // Load rides as passenger (completed requests)
        repo.getCompletedRidesAsPassenger(uid, new FirestoreRepository.Callback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                if (requests.isEmpty()) {
                    showEmpty("No completed rides yet");
                    return;
                }
                for (RideRequest req : requests) {
                    addRequestCard(req, "Completed", 0xFF4CAF50);
                }
            }
            @Override
            public void onFailure(String error) {
                showEmpty("Could not load rides");
            }
        });
    }

    private void loadCancelledRides() {
        ridesContainer.removeAllViews();
        String uid = session.getUid();

        repo.getCancelledRidesAsPassenger(uid, new FirestoreRepository.Callback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                if (!requests.isEmpty()) {
                    for (RideRequest req : requests) {
                        addRequestCard(req, "Cancelled", 0xFF9E9E9E);
                    }
                    return;
                }

                repo.getCancelledRidesAsRider(uid, new FirestoreRepository.Callback<List<Ride>>() {
                    @Override
                    public void onSuccess(List<Ride> rides) {
                        if (rides.isEmpty()) {
                            showEmpty("No cancelled rides");
                            return;
                        }
                        for (Ride ride : rides) {
                            addRideCard(ride, "Cancelled", 0xFF9E9E9E, false);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        showEmpty("Could not load cancelled rides");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                showEmpty("Could not load cancelled rides");
            }
        });
    }

    private void loadScheduledRides() {
        ridesContainer.removeAllViews();
        // Show active rides from Firestore
        repo.getActiveRidesForRider(session.getUid(), new FirestoreRepository.Callback<List<Ride>>() {
            @Override
            public void onSuccess(List<Ride> rides) {
                if (rides.isEmpty()) {
                    showEmpty("No scheduled rides");
                    return;
                }
                for (Ride ride : rides) {
                    addRideCard(ride, "Active", 0xFF2196F3, true);
                }
            }
            @Override
            public void onFailure(String error) {
                showEmpty("Could not load scheduled rides");
            }
        });
    }

    private void showEmpty(String msg) {
        ridesContainer.removeAllViews();
        TextView tv = new TextView(requireContext());
        tv.setText(msg);
        tv.setTextSize(14);
        tv.setTextColor(0xFF9E9E9E);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 80, 0, 80);
        ridesContainer.addView(tv);
    }

    private void addRequestCard(RideRequest req, String statusLabel, int statusColor) {
        LinearLayout card = new LinearLayout(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 24);
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(40, 32, 40, 32);
        card.setElevation(8f);

        // Route
        TextView route = new TextView(requireContext());
        route.setText(req.getPickupName() + " → " + req.getDropName());
        route.setTextSize(16);
        route.setTextColor(0xFF212121);
        route.setTypeface(null, android.graphics.Typeface.BOLD);

        // Rider + cost
        TextView sub = new TextView(requireContext());
        sub.setText("Rider: " + req.getRiderName() + " • ₹" + String.format("%.0f", req.getCostAgreed()));
        sub.setTextSize(13);
        sub.setTextColor(0xFF757575);
        sub.setPadding(0, 4, 0, 0);

        // Status
        TextView status = new TextView(requireContext());
        status.setText(statusLabel);
        status.setTextSize(12);
        status.setTextColor(statusColor);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setPadding(0, 12, 0, 0);

        card.addView(route);
        card.addView(sub);
        card.addView(status);
        ridesContainer.addView(card);
    }

    private void addRideCard(Ride ride, String statusLabel, int statusColor, boolean allowCancel) {
        LinearLayout card = new LinearLayout(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 24);
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(40, 32, 40, 32);
        card.setElevation(8f);

        TextView route = new TextView(requireContext());
        route.setText(ride.getFromName() + " → " + ride.getToName());
        route.setTextSize(16);
        route.setTextColor(0xFF212121);
        route.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView sub = new TextView(requireContext());
        String time = ride.getDepartureTime() != null
                ? new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    .format(ride.getDepartureTime().toDate())
                : "";
        sub.setText(ride.getVehicleType() + " • " + ride.getSeatsAvailable() + " seats • " + time);
        sub.setTextSize(13);
        sub.setTextColor(0xFF757575);
        sub.setPadding(0, 4, 0, 0);

        TextView status = new TextView(requireContext());
        status.setText(statusLabel);
        status.setTextSize(12);
        status.setTextColor(statusColor);
        status.setTypeface(null, android.graphics.Typeface.BOLD);
        status.setPadding(0, 12, 0, 0);

        card.addView(route);
        card.addView(sub);
        card.addView(status);

        if (allowCancel) {
            TextView cancelBtn = new TextView(requireContext());
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 92);
            cp.topMargin = 16;
            cancelBtn.setLayoutParams(cp);
            cancelBtn.setText("Cancel Ride");
            cancelBtn.setGravity(Gravity.CENTER);
            cancelBtn.setTextSize(14);
            cancelBtn.setTextColor(0xFFFFFFFF);
            cancelBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            cancelBtn.setBackgroundResource(R.drawable.bg_button_cyan);
            cancelBtn.setClickable(true);
            cancelBtn.setFocusable(true);

            cancelBtn.setOnClickListener(v -> cancelRide(ride));
            card.addView(cancelBtn);
        }

        ridesContainer.addView(card);
    }

    private void cancelRide(Ride ride) {
        repo.updateRideStatus(ride.getRideId(), Ride.STATUS_CANCELLED, new FirestoreRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                repo.updateRequestsForRide(ride.getRideId(), RideRequest.STATUS_CANCELLED,
                        new FirestoreRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Ride cancelled", Toast.LENGTH_SHORT).show();
                                loadScheduledRides();
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(getContext(), "Cancel failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Cancel failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}