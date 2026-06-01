package com.example.tradget;

import android.app.Application;

import com.google.android.libraries.places.api.Places;

/**
 * Application class — initializes singleton SDK clients exactly once at app start.
 * Places SDK is initialized here so neither fragments nor activities need to duplicate it.
 */
public class TradGetApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Google Places SDK once for the entire app lifecycle
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }
    }
}
