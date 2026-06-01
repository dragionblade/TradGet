package com.example.tradget;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Splash screen — routes the user based on authentication state:
 *  - Not logged in → AuthActivity
 *  - Already logged in → MainAppActivity (skip auth entirely)
 */
public class LauncherActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // Fade-in the splash content
        LinearLayout splashContent = findViewById(R.id.splashContent);
        if (splashContent != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(500);
            fadeIn.setFillAfter(true);
            splashContent.startAnimation(fadeIn);
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::routeUser, SPLASH_DURATION_MS);
    }

    /** Decides where to go after the splash: Auth or Main. */
    private void routeUser() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // User already authenticated — go straight to the app
            startActivity(new Intent(this, MainAppActivity.class));
        } else {
            // Not authenticated — show sign-in screen
            startActivity(new Intent(this, AuthActivity.class));
        }
        finish();
    }
}