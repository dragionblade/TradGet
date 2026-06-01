package com.example.tradget;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainAppActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);

        // Note: Places SDK is initialized in TradGetApplication — no need to init here.

        bottomNav = findViewById(R.id.bottomNav);

        if (!openChatIfRequested()) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HomeFragment())
                .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment selected = null;

            if(item.getItemId() == R.id.nav_home)
                selected = new HomeFragment();

            else if(item.getItemId() == R.id.nav_chat)
                selected = new ChatListFragment();

            else if(item.getItemId() == R.id.nav_history)
                selected = new HistoryFragment();

            else if(item.getItemId() == R.id.nav_profile)
                selected = new ProfileFragment();

            if(selected != null)
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, selected)
                        .commit();

            return true;
        });

    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openChatIfRequested();
    }

    private boolean openChatIfRequested() {
        String openChatId = getIntent().getStringExtra("open_chat_id");
        if (openChatId == null || openChatId.isEmpty()) return false;

        String openChatName = getIntent().getStringExtra("open_chat_name");
        String requestId = getIntent().getStringExtra("open_chat_request_id");
        String rideId = getIntent().getStringExtra("open_chat_ride_id");
        String riderId = getIntent().getStringExtra("open_chat_rider_id");
        String passengerId = getIntent().getStringExtra("open_chat_passenger_id");
        double costAgreed = getIntent().getDoubleExtra("open_chat_cost_agreed", 0);

        Bundle args = new Bundle();
        args.putString("chat_id", openChatId);
        args.putString("chat_name", openChatName != null ? openChatName : "Ride partner");
        args.putString("request_id", requestId != null ? requestId : "");
        args.putString("ride_id", rideId != null ? rideId : "");
        args.putString("rider_id", riderId != null ? riderId : "");
        args.putString("passenger_id", passengerId != null ? passengerId : "");
        args.putDouble("cost_agreed", costAgreed);

        ChatFragment chatFragment = new ChatFragment();
        chatFragment.setArguments(args);

        bottomNav.setSelectedItemId(R.id.nav_chat);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, chatFragment)
                .commit();
        return true;
    }
}