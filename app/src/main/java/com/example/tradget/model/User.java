package com.example.tradget.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Represents a TradGet user document stored in Firestore under {@code users/{uid}}.
 *
 * <p>Firebase requires a public no-arg constructor and public getters for auto-deserialization.
 */
@IgnoreExtraProperties
public class User {

    private String uid;          // Not stored in document — document ID
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private String universityId;
    private double rating;
    private int    totalRatings;
    private int    ridesCompleted;
    private double totalSaved;   // ₹ saved as a passenger
    private Timestamp createdAt;

    // ── Required no-arg constructor for Firestore deserialization ──────────────
    public User() {}

    public User(String uid, String name, String email) {
        this.uid   = uid;
        this.name  = name;
        this.email = email;
        this.rating         = 0.0;
        this.totalRatings   = 0;
        this.ridesCompleted = 0;
        this.totalSaved     = 0.0;
        this.createdAt      = Timestamp.now();
    }

    // ── Getters (Firebase needs these) ─────────────────────────────────────────
    @Exclude public String getUid()              { return uid; }
    public String getName()                      { return name; }
    public String getEmail()                     { return email; }
    public String getPhone()                     { return phone; }
    public String getProfileImageUrl()           { return profileImageUrl; }
    public String getUniversityId()              { return universityId; }
    public double getRating()                    { return rating; }
    public int    getTotalRatings()              { return totalRatings; }
    public int    getRidesCompleted()            { return ridesCompleted; }
    public double getTotalSaved()                { return totalSaved; }
    public Timestamp getCreatedAt()              { return createdAt; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setUid(String uid)                          { this.uid = uid; }
    public void setName(String name)                        { this.name = name; }
    public void setEmail(String email)                      { this.email = email; }
    public void setPhone(String phone)                      { this.phone = phone; }
    public void setProfileImageUrl(String url)              { this.profileImageUrl = url; }
    public void setUniversityId(String universityId)        { this.universityId = universityId; }
    public void setRating(double rating)                    { this.rating = rating; }
    public void setTotalRatings(int totalRatings)           { this.totalRatings = totalRatings; }
    public void setRidesCompleted(int ridesCompleted)       { this.ridesCompleted = ridesCompleted; }
    public void setTotalSaved(double totalSaved)            { this.totalSaved = totalSaved; }
    public void setCreatedAt(Timestamp createdAt)           { this.createdAt = createdAt; }

    /** Formatted rating string — "4.8" or "New" if no ratings yet. */
    @Exclude
    public String getRatingDisplay() {
        if (totalRatings == 0) return "New";
        return String.format("%.1f", rating);
    }
}
