package com.example.tradget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lightweight cache for the current authenticated user's data in SharedPreferences.
 *
 * <p>Avoids a Firestore round-trip every time a fragment needs the current user's
 * name or UID. Always backed by FirebaseAuth as the source of truth — clear on logout.
 */
public class SessionManager {

    private static final String PREF_NAME       = "tradget_session";
    private static final String KEY_UID         = "uid";
    private static final String KEY_NAME        = "name";
    private static final String KEY_EMAIL       = "email";
    private static final String KEY_PHONE       = "phone";
    private static final String KEY_RATING      = "rating";
    private static final String KEY_RIDES_DONE  = "rides_done";
    private static final String KEY_TOTAL_SAVED = "total_saved";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Write ──────────────────────────────────────────────────────────────────

    public void saveSession(String uid, String name, String email) {
        prefs.edit()
             .putString(KEY_UID,   uid)
             .putString(KEY_NAME,  name)
             .putString(KEY_EMAIL, email)
             .apply();
    }

    public void saveProfile(String phone, double rating, int ridesDone, double totalSaved) {
        prefs.edit()
             .putString(KEY_PHONE,       phone)
             .putFloat(KEY_RATING,       (float) rating)
             .putInt(KEY_RIDES_DONE,     ridesDone)
             .putFloat(KEY_TOTAL_SAVED,  (float) totalSaved)
             .apply();
    }

    /** Call on sign-out — wipes all cached data. */
    public void clearSession() {
        prefs.edit().clear().apply();
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return prefs.getString(KEY_UID, null) != null;
    }

    public String getUid()        { return prefs.getString(KEY_UID,   ""); }
    public String getName()       { return prefs.getString(KEY_NAME,  "User"); }
    public String getEmail()      { return prefs.getString(KEY_EMAIL, ""); }
    public String getPhone()      { return prefs.getString(KEY_PHONE, ""); }
    public double getRating()     { return prefs.getFloat(KEY_RATING,     0f); }
    public int    getRidesDone()  { return prefs.getInt(KEY_RIDES_DONE,   0); }
    public double getTotalSaved() { return prefs.getFloat(KEY_TOTAL_SAVED, 0f); }
}
