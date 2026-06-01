package com.example.tradget;

import android.util.Log;

import com.example.tradget.model.ChatMessage;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Centralised data-access layer for Firebase Realtime Database chat operations.
 *
 * <p>Chat rooms live at: {@code chats/{chatId}/messages/{messageId}}
 * where {@code chatId} is the deterministic ID from {@link com.example.tradget.model.RideRequest#getChatId()}.
 */
public class RealtimeChatRepository {

    private static final String TAG       = "ChatRepo";
    private static final String NODE_CHATS    = "chats";
    private static final String NODE_MESSAGES = "messages";

    private static RealtimeChatRepository sInstance;
    private final DatabaseReference dbRoot;

    /** Active Realtime DB listener — kept so it can be cleanly removed. */
    private ChildEventListener activeListener;
    private DatabaseReference  activeRef;

    private RealtimeChatRepository() {
        dbRoot = FirebaseDatabase.getInstance().getReference();
    }

    public static synchronized RealtimeChatRepository getInstance() {
        if (sInstance == null) sInstance = new RealtimeChatRepository();
        return sInstance;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Write
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Pushes a new message into the chat room.
     *
     * @param chatId  deterministic ID for the rider–passenger pair
     * @param message message object (senderId, text, timestamp already set)
     * @param cb      optional success/failure feedback
     */
    public void sendMessage(String chatId, ChatMessage message,
                            FirestoreRepository.SimpleCallback cb) {
        DatabaseReference msgRef = dbRoot
                .child(NODE_CHATS)
                .child(chatId)
                .child(NODE_MESSAGES)
                .push();                 // auto-generates a unique key

        message.setMessageId(msgRef.getKey());

        msgRef.setValue(message)
              .addOnSuccessListener(v -> { if (cb != null) cb.onSuccess(); })
              .addOnFailureListener(e -> {
                  Log.e(TAG, "sendMessage failed", e);
                  if (cb != null) cb.onFailure(e.getMessage());
              });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Read (real-time)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Attaches a real-time child-event listener to the message list for {@code chatId}.
     * Each new message triggers {@code cb.onSuccess(message)}.
     *
     * <p>Automatically detaches any previously attached listener first.
     * Call {@link #detachListener()} when leaving the chat screen.
     */
    public void listenToMessages(String chatId, FirestoreRepository.Callback<ChatMessage> cb) {
        // Clean up any previous listener first
        detachListener();

        activeRef = dbRoot
                .child(NODE_CHATS)
                .child(chatId)
                .child(NODE_MESSAGES);

        activeListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snap,
                                     @Nullable String previousChildName) {
                ChatMessage msg = snap.getValue(ChatMessage.class);
                if (msg != null) {
                    msg.setMessageId(snap.getKey());
                    cb.onSuccess(msg);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "listenToMessages cancelled: " + error.getMessage());
                cb.onFailure(error.getMessage());
            }
        };

        activeRef.addChildEventListener(activeListener);
    }

    /** Removes the currently active message listener. Call from {@code onStop} / {@code onDestroyView}. */
    public void detachListener() {
        if (activeRef != null && activeListener != null) {
            activeRef.removeEventListener(activeListener);
            activeRef      = null;
            activeListener = null;
        }
    }

    /** Marks all messages in a chat as read for the given user. */
    public void markAllRead(String chatId, String readerUid) {
        DatabaseReference msgRef = dbRoot
                .child(NODE_CHATS)
                .child(chatId)
                .child(NODE_MESSAGES);

        msgRef.get().addOnSuccessListener(snap -> {
            for (DataSnapshot child : snap.getChildren()) {
                ChatMessage msg = child.getValue(ChatMessage.class);
                if (msg != null && !msg.getSenderId().equals(readerUid) && !msg.isRead()) {
                    child.getRef().child("read").setValue(true);
                }
            }
        });
    }
}
