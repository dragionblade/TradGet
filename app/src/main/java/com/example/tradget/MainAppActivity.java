package com.example.tradget;


import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import com.google.android.libraries.places.api.Places;


import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainAppActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "API KEY");
        }


        bottomNav = findViewById(R.id.bottomNav);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new HomeFragment())
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment selected = null;

            if(item.getItemId() == R.id.nav_home)
                selected = new HomeFragment();

            else if(item.getItemId() == R.id.nav_chat)
                selected = new ChatFragment();

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
}
