package com.example.tradget;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.tradget.model.Ride;
import com.example.tradget.model.RideRequest;
import com.example.tradget.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralised data-access layer for all Cloud Firestore operations.
 *
 * <p>All methods are asynchronous and deliver results via simple callback interfaces
 * on the main thread (Firestore SDK already dispatches to main thread).
 */
public class FirestoreRepository {

    private static final String TAG = "FirestoreRepo";

    // ── Collection names ───────────────────────────────────────────────────────
    public static final String COL_USERS    = "users";
    public static final String COL_RIDES    = "rides";
    public static final String COL_REQUESTS = "rideRequests";

    private static FirestoreRepository sInstance;
    private final FirebaseFirestore db;

    private FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreRepository getInstance() {
        if (sInstance == null) sInstance = new FirestoreRepository();
        return sInstance;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Generic callbacks
    // ══════════════════════════════════════════════════════════════════════════

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // User operations
    // ══════════════════════════════════════════════════════════════════════════

    /** Creates or overwrites a user document. */
    public void createUser(User user, SimpleCallback cb) {
        db.collection(COL_USERS)
          .document(user.getUid())
          .set(user)
          .addOnSuccessListener(v -> cb.onSuccess())
          .addOnFailureListener(fail(cb));
    }

    /** Fetches a user document and deserialises it. */
    public void getUserProfile(String uid, Callback<User> cb) {
        db.collection(COL_USERS)
          .document(uid)
          .get()
          .addOnSuccessListener(snap -> {
              if (!snap.exists()) { cb.onFailure("User not found"); return; }
              User user = snap.toObject(User.class);
              if (user != null) user.setUid(snap.getId());
              cb.onSuccess(user);
          })
          .addOnFailureListener(fail(cb));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ride operations (Rider)
    // ══════════════════════════════════════════════════════════════════════════

    /** Publishes a new ride offer — auto-generates the Firestore document ID. */
    public void publishRide(Ride ride, Callback<String> cb) {
        ride.setStatus(Ride.STATUS_ACTIVE);
        ride.setCreatedAt(Timestamp.now());

        db.collection(COL_RIDES)
          .add(ride)
          .addOnSuccessListener(ref -> cb.onSuccess(ref.getId()))
          .addOnFailureListener(fail(cb));
    }

    /** Returns all rides currently offered by a specific rider (any status). */
    public void getActiveRidesForRider(String riderId, Callback<List<Ride>> cb) {
        db.collection(COL_RIDES)
          .whereEqualTo("riderId", riderId)
          .whereEqualTo("status", Ride.STATUS_ACTIVE)
          .orderBy("createdAt", Query.Direction.DESCENDING)
          .get()
          .addOnSuccessListener(snap -> {
              List<Ride> list = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  Ride r = doc.toObject(Ride.class);
                  r.setRideId(doc.getId());
                  list.add(r);
              }
              cb.onSuccess(list);
          })
          .addOnFailureListener(fail(cb));
    }

    /** Fetches a single ride document by id. */
    public void getRideById(String rideId, Callback<Ride> cb) {
        db.collection(COL_RIDES)
          .document(rideId)
          .get()
          .addOnSuccessListener(snap -> {
              if (!snap.exists()) { cb.onFailure("Ride not found"); return; }
              Ride r = snap.toObject(Ride.class);
              if (r != null) r.setRideId(snap.getId());
              cb.onSuccess(r);
          })
          .addOnFailureListener(fail(cb));
    }

    /** Updates seats available for a ride. */
    public void updateRideSeats(String rideId, int seatsAvailable, SimpleCallback cb) {
        db.collection(COL_RIDES)
          .document(rideId)
          .update("seatsAvailable", seatsAvailable)
          .addOnSuccessListener(v -> cb.onSuccess())
          .addOnFailureListener(fail(cb));
    }

        /** Increments ride stats for a user (rides completed and total saved). */
        public void incrementUserStats(String uid, int ridesDelta, double savedDelta) {
                if (uid == null || uid.isEmpty()) return;
                db.collection(COL_USERS)
                    .document(uid)
                    .update(
                                    "ridesCompleted", FieldValue.increment(ridesDelta),
                                    "totalSaved", FieldValue.increment(savedDelta)
                    )
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update user stats", e));
        }

        /** Updates the rating and total ratings count for a user. */
        public void submitUserRating(String uid, double newRating, SimpleCallback cb) {
            if (uid == null || uid.isEmpty()) {
                if (cb != null) cb.onFailure("Missing user id");
                return;
            }

            DocumentReference ref = db.collection(COL_USERS).document(uid);
            db.runTransaction((Transaction.Function<Void>) transaction -> {
                double rating = 0.0;
                long totalRatings = 0;
                if (transaction.get(ref).exists()) {
                    Double r = transaction.get(ref).getDouble("rating");
                    Long t = transaction.get(ref).getLong("totalRatings");
                    if (r != null) rating = r;
                    if (t != null) totalRatings = t;
                }

                double updated = (rating * totalRatings + newRating) / (totalRatings + 1);
                transaction.update(ref,
                        "rating", updated,
                        "totalRatings", totalRatings + 1);
                return null;
            }).addOnSuccessListener(v -> {
                if (cb != null) cb.onSuccess();
            }).addOnFailureListener(e -> {
                if (cb != null) cb.onFailure(e.getMessage());
            });
        }

        /** Updates all requests for a ride to a new status (e.g., when rider cancels). */
        public void updateRequestsForRide(String rideId, String status, SimpleCallback cb) {
            db.collection(COL_REQUESTS)
              .whereEqualTo("rideId", rideId)
              .get()
              .addOnSuccessListener(snap -> {
                  List<DocumentReference> refs = new ArrayList<>();
                  for (QueryDocumentSnapshot doc : snap) {
                      refs.add(doc.getReference());
                  }
                  if (refs.isEmpty()) {
                      if (cb != null) cb.onSuccess();
                      return;
                  }
                  db.runBatch(batch -> {
                      for (DocumentReference ref : refs) {
                          batch.update(ref, "status", status);
                      }
                  }).addOnSuccessListener(v -> {
                      if (cb != null) cb.onSuccess();
                  }).addOnFailureListener(e -> {
                      if (cb != null) cb.onFailure(e.getMessage());
                  });
              })
              .addOnFailureListener(fail(cb));
        }

    public void updateRideStatus(String rideId, String newStatus, SimpleCallback cb) {
        db.collection(COL_RIDES)
          .document(rideId)
          .update("status", newStatus)
          .addOnSuccessListener(v -> cb.onSuccess())
          .addOnFailureListener(fail(cb));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ride search (Passenger)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Fetches all ACTIVE rides and filters client-side for a matching destination.
     * Client-side filtering is fine at university scale (dozens not millions of rides).
     */
    public void searchRides(String fromName, String toName, Callback<List<Ride>> cb) {
        db.collection(COL_RIDES)
          .whereEqualTo("fromName", fromName)
          .whereEqualTo("toName", toName)
          .whereEqualTo("status", Ride.STATUS_ACTIVE)
          .limit(50)
          .get()
          .addOnSuccessListener(snap -> {
              List<Ride> results = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  Ride r = doc.toObject(Ride.class);
                  r.setRideId(doc.getId());
                  results.add(r);
              }
              cb.onSuccess(results);
          })
          .addOnFailureListener(fail(cb));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ride request operations
    // ══════════════════════════════════════════════════════════════════════════

    /** Creates a new ride request. Returns the generated requestId. */
    public void createRideRequest(RideRequest request, Callback<String> cb) {
        request.setStatus(RideRequest.STATUS_PENDING);
        request.setRequestedAt(Timestamp.now());

        db.collection(COL_REQUESTS)
          .add(request)
          .addOnSuccessListener(ref -> cb.onSuccess(ref.getId()))
          .addOnFailureListener(fail(cb));
    }

    /**
     * Attaches a real-time listener to a specific ride request document.
     * Returns a {@link ListenerRegistration} — call {@code remove()} to detach.
     */
    public ListenerRegistration listenToRideRequest(String requestId, Callback<RideRequest> cb) {
        return db.collection(COL_REQUESTS)
                 .document(requestId)
                 .addSnapshotListener((snap, err) -> {
                     if (err != null) { cb.onFailure(err.getMessage()); return; }
                     if (snap == null || !snap.exists()) return;
                     RideRequest req = snap.toObject(RideRequest.class);
                     if (req != null) req.setRequestId(snap.getId());
                     cb.onSuccess(req);
                 });
    }

    /** Updates the status of a ride request (accept / reject / cancel / complete). */
    public void updateRequestStatus(String requestId, String status, SimpleCallback cb) {
        db.collection(COL_REQUESTS)
          .document(requestId)
          .update("status", status)
          .addOnSuccessListener(v -> cb.onSuccess())
          .addOnFailureListener(fail(cb));
    }

    /**
     * Returns PENDING requests sent to a specific rider — shown in the rider's
     * home feed so they can accept or reject passengers.
     */
    public void getPendingRequestsForRider(String riderId, Callback<List<RideRequest>> cb) {
        db.collection(COL_REQUESTS)
          .whereEqualTo("riderId", riderId)
          .whereEqualTo("status", RideRequest.STATUS_PENDING)
          .get()
          .addOnSuccessListener(snap -> {
              List<RideRequest> list = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  RideRequest req = doc.toObject(RideRequest.class);
                  req.setRequestId(doc.getId());
                  list.add(req);
              }
              cb.onSuccess(list);
          })
          .addOnFailureListener(fail(cb));
    }

    /** Returns ACCEPTED requests for the current user (used to list active chats). */
    public void getAcceptedRequestsForUser(String uid, Callback<List<RideRequest>> cb) {
        // Passenger's accepted requests
        db.collection(COL_REQUESTS)
          .whereEqualTo("passengerId", uid)
          .whereEqualTo("status", RideRequest.STATUS_ACCEPTED)
          .get()
          .addOnSuccessListener(snap -> {
              List<RideRequest> list = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  RideRequest req = doc.toObject(RideRequest.class);
                  req.setRequestId(doc.getId());
                  list.add(req);
              }
              // Also fetch rider's accepted requests
              db.collection(COL_REQUESTS)
                .whereEqualTo("riderId", uid)
                .whereEqualTo("status", RideRequest.STATUS_ACCEPTED)
                .get()
                .addOnSuccessListener(snap2 -> {
                    for (QueryDocumentSnapshot doc : snap2) {
                        RideRequest req = doc.toObject(RideRequest.class);
                        req.setRequestId(doc.getId());
                        list.add(req);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(fail(cb));
          })
          .addOnFailureListener(fail(cb));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // History queries
    // ══════════════════════════════════════════════════════════════════════════

    public void getCompletedRidesAsPassenger(String passengerId, Callback<List<RideRequest>> cb) {
        db.collection(COL_REQUESTS)
          .whereEqualTo("passengerId", passengerId)
          .whereEqualTo("status", RideRequest.STATUS_COMPLETED)
          .limit(30)
          .get()
          .addOnSuccessListener(snap -> {
              List<RideRequest> list = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  RideRequest req = doc.toObject(RideRequest.class);
                  req.setRequestId(doc.getId());
                  list.add(req);
              }
              cb.onSuccess(list);
          })
          .addOnFailureListener(fail(cb));
    }

    public void getCompletedRidesAsRider(String riderId, Callback<List<Ride>> cb) {
        db.collection(COL_RIDES)
          .whereEqualTo("riderId", riderId)
          .whereEqualTo("status", Ride.STATUS_COMPLETED)
          .orderBy("createdAt", Query.Direction.DESCENDING)
          .limit(30)
          .get()
          .addOnSuccessListener(snap -> {
              List<Ride> list = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  Ride r = doc.toObject(Ride.class);
                  r.setRideId(doc.getId());
                  list.add(r);
              }
              cb.onSuccess(list);
          })
          .addOnFailureListener(fail(cb));
    }

    public void getCancelledRidesAsPassenger(String passengerId, Callback<List<RideRequest>> cb) {
        db.collection(COL_REQUESTS)
          .whereEqualTo("passengerId", passengerId)
          .whereEqualTo("status", RideRequest.STATUS_CANCELLED)
          .orderBy("requestedAt", Query.Direction.DESCENDING)
          .get()
          .addOnSuccessListener(snap -> {
              List<RideRequest> list = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  RideRequest req = doc.toObject(RideRequest.class);
                  req.setRequestId(doc.getId());
                  list.add(req);
              }
              cb.onSuccess(list);
          })
          .addOnFailureListener(fail(cb));
    }

    public void getCancelledRidesAsRider(String riderId, Callback<List<Ride>> cb) {
        db.collection(COL_RIDES)
          .whereEqualTo("riderId", riderId)
          .whereEqualTo("status", Ride.STATUS_CANCELLED)
          .orderBy("createdAt", Query.Direction.DESCENDING)
          .limit(30)
          .get()
          .addOnSuccessListener(snap -> {
              List<Ride> list = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  Ride r = doc.toObject(Ride.class);
                  r.setRideId(doc.getId());
                  list.add(r);
              }
              cb.onSuccess(list);
          })
          .addOnFailureListener(fail(cb));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Favorites
    // ══════════════════════════════════════════════════════════════════════════

    /** Adds a rider to passenger's favorites. */
    public void addFavorite(String passengerId, String riderId, SimpleCallback cb) {
        db.collection(COL_USERS)
          .document(passengerId)
          .collection("favorites")
          .document(riderId)
          .set(new java.util.HashMap<String, Object>() {{
              put("riderId", riderId);
              put("addedAt", Timestamp.now());
          }})
          .addOnSuccessListener(v -> cb.onSuccess())
          .addOnFailureListener(fail(cb));
    }

    /** Removes a rider from passenger's favorites. */
    public void removeFavorite(String passengerId, String riderId, SimpleCallback cb) {
        db.collection(COL_USERS)
          .document(passengerId)
          .collection("favorites")
          .document(riderId)
          .delete()
          .addOnSuccessListener(v -> cb.onSuccess())
          .addOnFailureListener(fail(cb));
    }

    /** Gets all favorite riders for a passenger. */
    public void getFavoriteRiders(String passengerId, Callback<List<String>> cb) {
        db.collection(COL_USERS)
          .document(passengerId)
          .collection("favorites")
          .get()
          .addOnSuccessListener(snap -> {
              List<String> favorites = new ArrayList<>();
              for (QueryDocumentSnapshot doc : snap) {
                  String riderId = doc.getString("riderId");
                  if (riderId != null) favorites.add(riderId);
              }
              cb.onSuccess(favorites);
          })
          .addOnFailureListener(fail(cb));
    }

    /** Checks if a rider is in passenger's favorites. */
    public void isFavorite(String passengerId, String riderId, Callback<Boolean> cb) {
        db.collection(COL_USERS)
          .document(passengerId)
          .collection("favorites")
          .document(riderId)
          .get()
          .addOnSuccessListener(snap -> cb.onSuccess(snap.exists()))
          .addOnFailureListener(fail(cb));
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    /** Converts a Firebase OnFailureListener to the repo's Callback pattern. */
    private <T> OnFailureListener fail(Callback<T> cb) {
        return e -> {
            Log.e(TAG, "Firestore error", e);
            cb.onFailure(e.getMessage());
        };
    }

    private OnFailureListener fail(SimpleCallback cb) {
        return e -> {
            Log.e(TAG, "Firestore error", e);
            cb.onFailure(e.getMessage());
        };
    }
}
