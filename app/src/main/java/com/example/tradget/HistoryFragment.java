package com.example.tradget;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

public class HistoryFragment extends Fragment {

    TextView tabCompleted, tabCancelled, tabScheduled;
    View view;

    public HistoryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history, container, false);

        tabCompleted = view.findViewById(R.id.tabCompleted);
        tabCancelled = view.findViewById(R.id.tabCancelled);
        tabScheduled = view.findViewById(R.id.tabScheduled);

        // Set up tab click listeners
        tabCompleted.setOnClickListener(v -> switchTab("completed"));
        tabCancelled.setOnClickListener(v -> switchTab("cancelled"));
        tabScheduled.setOnClickListener(v -> switchTab("scheduled"));

        return view;
    }

    private void switchTab(String tab) {
        // Reset all tabs
        tabCompleted.setTextColor(0xFF757575);
        tabCompleted.setBackgroundColor(0xFFFFFFFF);
        tabCancelled.setTextColor(0xFF757575);
        tabCancelled.setBackgroundColor(0xFFFFFFFF);
        tabScheduled.setTextColor(0xFF757575);
        tabScheduled.setBackgroundColor(0xFFFFFFFF);

        // Highlight selected tab
        switch (tab) {
            case "completed":
                tabCompleted.setTextColor(0xFF00BCD4);
                tabCompleted.setBackgroundColor(0xFFE3F2FD);
                Toast.makeText(getContext(), "Showing completed rides", Toast.LENGTH_SHORT).show();
                break;
            case "cancelled":
                tabCancelled.setTextColor(0xFF00BCD4);
                tabCancelled.setBackgroundColor(0xFFE3F2FD);
                Toast.makeText(getContext(), "Showing cancelled rides", Toast.LENGTH_SHORT).show();
                break;
            case "scheduled":
                tabScheduled.setTextColor(0xFF00BCD4);
                tabScheduled.setBackgroundColor(0xFFE3F2FD);
                Toast.makeText(getContext(), "Showing scheduled rides", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}