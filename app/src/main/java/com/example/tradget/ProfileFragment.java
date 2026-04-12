package com.example.tradget;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;
import android.widget.Toast;

public class ProfileFragment extends Fragment {

    LinearLayout menuRideHistory, menuPaymentMethods, menuEmergency, menuSettings, menuHelp, logoutButton;
    View view;

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        menuRideHistory = view.findViewById(R.id.menuRideHistory);
        menuPaymentMethods = view.findViewById(R.id.menuPaymentMethods);
        menuEmergency = view.findViewById(R.id.menuEmergency);
        menuSettings = view.findViewById(R.id.menuSettings);
        menuHelp = view.findViewById(R.id.menuHelp);
        logoutButton = view.findViewById(R.id.logoutButton);

        menuRideHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Ride History clicked", Toast.LENGTH_SHORT).show());
        menuPaymentMethods.setOnClickListener(v -> Toast.makeText(getContext(), "Payment Methods clicked", Toast.LENGTH_SHORT).show());
        menuEmergency.setOnClickListener(v -> Toast.makeText(getContext(), "Emergency Contacts clicked", Toast.LENGTH_SHORT).show());
        menuSettings.setOnClickListener(v -> Toast.makeText(getContext(), "Settings clicked", Toast.LENGTH_SHORT).show());
        menuHelp.setOnClickListener(v -> Toast.makeText(getContext(), "Help & Support clicked", Toast.LENGTH_SHORT).show());
        logoutButton.setOnClickListener(v -> Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show());

        return view;
    }
}