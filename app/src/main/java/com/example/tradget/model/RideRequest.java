package com.example.tradget.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Represents a ride request document in Firestore under {@code rideRequests/{requestId}}.
 *
 * <p>Status lifecycle: PENDING → ACCEPTED → (ride completes) → COMPLETED
 *                                         → REJECTED
 *               or   PENDING → CANCELLED  (passenger cancels)
 */
@IgnoreExtraProperties
public class RideRequest {

    public static final String STATUS_PENDING   = "PENDING";
    public static final String STATUS_ACCEPTED  = "ACCEPTED";
    public static final String STATUS_REJECTED  = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String    requestId;      // Document ID
    private String    rideId;
    private String    passengerId;
    private String    passengerName;
    private String    riderId;
    private String    riderName;
    private String    status;
    private String    pickupName;
    private String    dropName;
    private double    costAgreed;
    private Timestamp requestedAt;

    public RideRequest() {}

    // ── Getters ────────────────────────────────────────────────────────────────
    @Exclude public String getRequestId()    { return requestId; }
    public String getRideId()               { return rideId; }
    public String getPassengerId()          { return passengerId; }
    public String getPassengerName()        { return passengerName; }
    public String getRiderId()              { return riderId; }
    public String getRiderName()            { return riderName; }
    public String getStatus()               { return status; }
    public String getPickupName()           { return pickupName; }
    public String getDropName()             { return dropName; }
    public double getCostAgreed()           { return costAgreed; }
    public Timestamp getRequestedAt()       { return requestedAt; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setRequestId(String requestId)       { this.requestId = requestId; }
    public void setRideId(String rideId)             { this.rideId = rideId; }
    public void setPassengerId(String passengerId)   { this.passengerId = passengerId; }
    public void setPassengerName(String name)        { this.passengerName = name; }
    public void setRiderId(String riderId)           { this.riderId = riderId; }
    public void setRiderName(String riderName)       { this.riderName = riderName; }
    public void setStatus(String status)             { this.status = status; }
    public void setPickupName(String pickupName)     { this.pickupName = pickupName; }
    public void setDropName(String dropName)         { this.dropName = dropName; }
    public void setCostAgreed(double costAgreed)     { this.costAgreed = costAgreed; }
    public void setRequestedAt(Timestamp t)          { this.requestedAt = t; }

    /** Generates a deterministic chat ID for a rider–passenger pair. */
    @Exclude
    public String getChatId() {
        if (riderId == null || passengerId == null) return "";
        // Sort alphabetically so the same pair always produces the same chatId
        return riderId.compareTo(passengerId) < 0
                ? riderId + "_" + passengerId
                : passengerId + "_" + riderId;
    }
}
