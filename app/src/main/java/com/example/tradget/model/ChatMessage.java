package com.example.tradget.model;

import com.google.firebase.firestore.Exclude;

/**
 * Represents a single chat message stored in Firebase Realtime Database under:
 * {@code chats/{chatId}/messages/{messageId}}
 */
public class ChatMessage {

    private String  messageId;    // Not stored — set from the database key
    private String  senderId;
    private String  senderName;
    private String  text;
    private long    timestamp;    // System.currentTimeMillis()
    private boolean read;

    // Required no-arg constructor for Firebase deserialization
    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String text) {
        this.senderId   = senderId;
        this.senderName = senderName;
        this.text       = text;
        this.timestamp  = System.currentTimeMillis();
        this.read       = false;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    @Exclude public String getMessageId()  { return messageId; }
    public String  getSenderId()           { return senderId; }
    public String  getSenderName()         { return senderName; }
    public String  getText()               { return text; }
    public long    getTimestamp()          { return timestamp; }
    public boolean isRead()                { return read; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setMessageId(String messageId)  { this.messageId = messageId; }
    public void setSenderId(String senderId)    { this.senderId = senderId; }
    public void setSenderName(String name)      { this.senderName = name; }
    public void setText(String text)            { this.text = text; }
    public void setTimestamp(long timestamp)    { this.timestamp = timestamp; }
    public void setRead(boolean read)           { this.read = read; }

    /** Returns time formatted as HH:MM for display in chat bubbles. */
    @Exclude
    public String getTimeDisplay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return String.format(java.util.Locale.getDefault(),
                "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY),
                             cal.get(java.util.Calendar.MINUTE));
    }
}
