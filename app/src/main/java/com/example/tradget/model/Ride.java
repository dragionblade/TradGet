package com.example.tradget.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Represents a ride offer document stored in Firestore under {@code rides/{rideId}}.
 *
 * <p>Status lifecycle: ACTIVE → IN_PROGRESS → COMPLETED | CANCELLED
 */
@IgnoreExtraProperties
public class Ride {

    public static final String STATUS_ACTIVE      = "ACTIVE";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED   = "COMPLETED";
    public static final String STATUS_CANCELLED   = "CANCELLED";

    private String    rideId;         // Document ID — not stored in document body
    private String    riderId;
    private String    riderName;
    private double    riderRating;
    private String    vehicleType;    // "Bike" | "Scooty" | "Car"
    private String    fromName;
    private double    fromLat;
    private double    fromLng;
    private String    toName;
    private double    toLat;
    private double    toLng;
    private Timestamp departureTime;
    private int       seatsAvailable;
    private double    fuelPrice;      // ₹ per litre
    private double    distanceKm;
    private double    costPerPerson;  // computed at publish time
    private String    status;
    private Timestamp createdAt;

    // Required no-arg constructor
    public Ride() {}

    // ── Getters ────────────────────────────────────────────────────────────────
    @Exclude public String getRideId()     { return rideId; }
    public String  getRiderId()            { return riderId; }
    public String  getRiderName()          { return riderName; }
    public double  getRiderRating()        { return riderRating; }
    public String  getVehicleType()        { return vehicleType; }
    public String  getFromName()           { return fromName; }
    public double  getFromLat()            { return fromLat; }
    public double  getFromLng()            { return fromLng; }
    public String  getToName()             { return toName; }
    public double  getToLat()              { return toLat; }
    public double  getToLng()              { return toLng; }
    public Timestamp getDepartureTime()    { return departureTime; }
    public int     getSeatsAvailable()     { return seatsAvailable; }
    public double  getFuelPrice()          { return fuelPrice; }
    public double  getDistanceKm()         { return distanceKm; }
    public double  getCostPerPerson()      { return costPerPerson; }
    public String  getStatus()             { return status; }
    public Timestamp getCreatedAt()        { return createdAt; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setRideId(String rideId)               { this.rideId = rideId; }
    public void setRiderId(String riderId)             { this.riderId = riderId; }
    public void setRiderName(String riderName)         { this.riderName = riderName; }
    public void setRiderRating(double riderRating)     { this.riderRating = riderRating; }
    public void setVehicleType(String vehicleType)     { this.vehicleType = vehicleType; }
    public void setFromName(String fromName)           { this.fromName = fromName; }
    public void setFromLat(double fromLat)             { this.fromLat = fromLat; }
    public void setFromLng(double fromLng)             { this.fromLng = fromLng; }
    public void setToName(String toName)               { this.toName = toName; }
    public void setToLat(double toLat)                 { this.toLat = toLat; }
    public void setToLng(double toLng)                 { this.toLng = toLng; }
    public void setDepartureTime(Timestamp t)          { this.departureTime = t; }
    public void setSeatsAvailable(int seats)           { this.seatsAvailable = seats; }
    public void setFuelPrice(double fuelPrice)         { this.fuelPrice = fuelPrice; }
    public void setDistanceKm(double distanceKm)       { this.distanceKm = distanceKm; }
    public void setCostPerPerson(double cost)          { this.costPerPerson = cost; }
    public void setStatus(String status)               { this.status = status; }
    public void setCreatedAt(Timestamp createdAt)      { this.createdAt = createdAt; }

    @Exclude
    public String getRatingDisplay() {
        if (riderRating <= 0) return "New";
        return String.format("%.1f ★", riderRating);
    }

    @Exclude
    public String getCostDisplay() {
        return String.format("₹%.0f", costPerPerson);
    }
}
