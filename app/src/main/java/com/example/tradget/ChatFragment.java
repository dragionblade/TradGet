package com.example.tradget;

import android.os.Bundle;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.tradget.model.ChatMessage;

/**
 * Live chat screen backed by Firebase Realtime Database.
 *
 * <p>Arguments expected (via {@link #setArguments(Bundle)}):
 * <ul>
 *   <li>{@code chat_id}   — deterministic chat room ID</li>
 *   <li>{@code chat_name} — display name of the other participant</li>
 * </ul>
 */
public class ChatFragment extends Fragment {

    private EditText     chatInput;
    private ImageView    sendButton;
    private ImageView    closeButton;
    private LinearLayout chatMessagesContainer;
    private ScrollView   chatScrollView;
    private TextView     chatHeaderName, chatHeaderStatus;
    private LinearLayout chatActionBar;
    private TextView     completeRideButton;
    private TextView     cancelRideButton;

    private String chatId   = "";
    private String chatName = "Rider";
    private String requestId = "";
    private String rideId = "";
    private String riderId = "";
    private String passengerId = "";
    private double costAgreed = 0;

    private SessionManager         session;
    private RealtimeChatRepository chatRepo;

    public ChatFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Read arguments
        if (getArguments() != null) {
            chatId   = getArguments().getString("chat_id",   "");
            chatName = getArguments().getString("chat_name", "Rider");
            requestId = getArguments().getString("request_id", "");
            rideId = getArguments().getString("ride_id", "");
            riderId = getArguments().getString("rider_id", "");
            passengerId = getArguments().getString("passenger_id", "");
            costAgreed = getArguments().getDouble("cost_agreed", 0);
        }

        session  = new SessionManager(requireContext());
        chatRepo = RealtimeChatRepository.getInstance();

        bindViews(view);
        if (chatMessagesContainer != null) {
            chatMessagesContainer.removeAllViews();
        }
        updateHeader();

        if (chatActionBar != null && completeRideButton != null && !requestId.isEmpty()) {
            chatActionBar.setVisibility(View.VISIBLE);
            completeRideButton.setOnClickListener(v -> completeRide());
            if (cancelRideButton != null) {
                cancelRideButton.setOnClickListener(v -> cancelRide());
            }
        }

        if (!chatId.isEmpty()) {
            attachMessageListener();
        }

        if (!requestId.isEmpty()) {
            attachRequestStatusListener();
        }

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    requireActivity().finish();
                }
            });
        }

        sendButton.setOnClickListener(v -> sendMessage());
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        chatRepo.detachListener();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Setup
    // ══════════════════════════════════════════════════════════════════════════

    private void bindViews(View view) {
        chatInput             = view.findViewById(R.id.chatInput);
        sendButton            = view.findViewById(R.id.sendButton);
        closeButton           = view.findViewById(R.id.chatCloseButton);
        chatMessagesContainer = view.findViewById(R.id.chatMessagesContainer);
        chatScrollView        = view.findViewById(R.id.chatScrollView);
        chatHeaderName        = view.findViewById(R.id.chatHeaderName);
        chatHeaderStatus      = view.findViewById(R.id.chatHeaderStatus);
        chatActionBar         = view.findViewById(R.id.chatActionBar);
        completeRideButton    = view.findViewById(R.id.completeRideButton);
        cancelRideButton      = view.findViewById(R.id.cancelRideButton);
    }

    private void updateHeader() {
        if (chatHeaderName   != null) chatHeaderName.setText(chatName);
        if (chatHeaderStatus != null) chatHeaderStatus.setText("Active ride partner");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Real-time message listener
    // ══════════════════════════════════════════════════════════════════════════

    private void attachMessageListener() {
        chatRepo.listenToMessages(chatId, new FirestoreRepository.Callback<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage message) {
                boolean isMine = session.getUid().equals(message.getSenderId());
                addMessageBubble(message.getText(), message.getTimeDisplay(), isMine);
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), "Chat error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Mark messages as read
        chatRepo.markAllRead(chatId, session.getUid());
    }

    private void attachRequestStatusListener() {
        FirestoreRepository repo = FirestoreRepository.getInstance();
        repo.listenToRideRequest(requestId, new FirestoreRepository.Callback<com.example.tradget.model.RideRequest>() {
            @Override
            public void onSuccess(com.example.tradget.model.RideRequest req) {
                if (req.getStatus() != null) {
                    switch (req.getStatus()) {
                        case com.example.tradget.model.RideRequest.STATUS_COMPLETED:
                            if (chatHeaderStatus != null) {
                                chatHeaderStatus.setText("✅ Ride completed");
                            }
                            // Auto-prompt rating if passenger hasn't rated yet
                            if (!session.getUid().equals(req.getRiderId())) {
                                maybePromptRating(repo);
                            }
                            break;
                        case com.example.tradget.model.RideRequest.STATUS_CANCELLED:
                            if (chatHeaderStatus != null) {
                                chatHeaderStatus.setText("❌ Ride cancelled");
                            }
                            break;
                    }
                }
            }
            @Override
            public void onFailure(String error) {
                // Silently ignore listener errors
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Send message
    // ══════════════════════════════════════════════════════════════════════════

    private void sendMessage() {
        String text = chatInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(getContext(), "Please type a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId.isEmpty()) {
            Toast.makeText(getContext(), "No active chat selected", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage message = new ChatMessage(session.getUid(), session.getName(), text);
        chatInput.setText("");

        chatRepo.sendMessage(chatId, message, new FirestoreRepository.SimpleCallback() {
            @Override public void onSuccess() {}   // Message appears via listener
            @Override public void onFailure(String error) {
                Toast.makeText(requireContext(), "Send failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI — build message bubbles
    // ══════════════════════════════════════════════════════════════════════════

    private void addMessageBubble(String text, String time, boolean isSent) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                isSent ? 80 : 0, 0,
                isSent ? 0 : 80, 16);
        params.gravity = isSent ? android.view.Gravity.END : android.view.Gravity.START;LinearLayout bubble = new LinearLayout(getContext());
        bubble.setLayoutParams(params);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(20, 16, 20, 12);
        bubble.setBackgroundColor(isSent ? 0xFF00BCD4 : 0xFFFFFFFF);

        TextView messageText = new TextView(getContext());
        messageText.setText(text);
        messageText.setTextSize(14);
        messageText.setTextColor(isSent ? 0xFFFFFFFF : 0xFF212121);

        TextView timeText = new TextView(getContext());
        timeText.setText(time);
        timeText.setTextSize(10);
        timeText.setTextColor(isSent ? 0xAAFFFFFF : 0xFF9E9E9E);
        timeText.setGravity(android.view.Gravity.END);
        timeText.setPadding(0, 6, 0, 0);

        bubble.addView(messageText);
        bubble.addView(timeText);

        chatMessagesContainer.addView(bubble);
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void completeRide() {
        if (requestId.isEmpty() || rideId.isEmpty()) {
            Toast.makeText(getContext(), "Missing ride details", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreRepository repo = FirestoreRepository.getInstance();
        setActionButtonsEnabled(false);

        repo.updateRequestStatus(requestId, com.example.tradget.model.RideRequest.STATUS_COMPLETED,
                new FirestoreRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        repo.updateRideStatus(rideId, com.example.tradget.model.Ride.STATUS_COMPLETED,
                                new FirestoreRepository.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        updateUserStats(repo);
                                        maybePromptRating(repo);
                                        if (chatHeaderStatus != null) {
                                            chatHeaderStatus.setText("Ride completed");
                                        }
                                        Toast.makeText(getContext(), "Ride completed", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        setActionButtonsEnabled(true);
                                        Toast.makeText(getContext(), "Update failed: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        setActionButtonsEnabled(true);
                        Toast.makeText(getContext(), "Update failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cancelRide() {
        if (requestId.isEmpty()) {
            Toast.makeText(getContext(), "Missing request details", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreRepository repo = FirestoreRepository.getInstance();
        setActionButtonsEnabled(false);

        boolean isRider = session.getUid().equals(riderId);
        if (isRider && !rideId.isEmpty()) {
            repo.updateRideStatus(rideId, com.example.tradget.model.Ride.STATUS_CANCELLED,
                    new FirestoreRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            repo.updateRequestsForRide(rideId,
                                    com.example.tradget.model.RideRequest.STATUS_CANCELLED,
                                    new FirestoreRepository.SimpleCallback() {
                                        @Override
                                        public void onSuccess() {
                                            if (chatHeaderStatus != null) {
                                                chatHeaderStatus.setText("Ride cancelled");
                                            }
                                            Toast.makeText(getContext(), "Ride cancelled", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            setActionButtonsEnabled(true);
                                            Toast.makeText(getContext(), "Cancel failed: " + error, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(String error) {
                            setActionButtonsEnabled(true);
                            Toast.makeText(getContext(), "Cancel failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            repo.updateRequestStatus(requestId, com.example.tradget.model.RideRequest.STATUS_CANCELLED,
                    new FirestoreRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (chatHeaderStatus != null) {
                                chatHeaderStatus.setText("Ride cancelled");
                            }
                            Toast.makeText(getContext(), "Ride cancelled", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            setActionButtonsEnabled(true);
                            Toast.makeText(getContext(), "Cancel failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateUserStats(FirestoreRepository repo) {
        if (!riderId.isEmpty()) {
            repo.incrementUserStats(riderId, 1, 0);
        }
        if (!passengerId.isEmpty()) {
            repo.incrementUserStats(passengerId, 1, costAgreed);
        }
    }

    private void maybePromptRating(FirestoreRepository repo) {
        boolean isPassenger = session.getUid().equals(passengerId);
        if (!isPassenger || riderId.isEmpty()) return;

        String[] options = new String[] {"5", "4", "3", "2", "1"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Rate your rider")
                .setItems(options, (dialog, which) -> {
                    double rating = Double.parseDouble(options[which]);
                    repo.submitUserRating(riderId, rating, new FirestoreRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Thanks for rating", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(getContext(), "Rating failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setCancelable(true)
                .show();
    }

    private void setActionButtonsEnabled(boolean enabled) {
        if (completeRideButton != null) completeRideButton.setEnabled(enabled);
        if (cancelRideButton != null) cancelRideButton.setEnabled(enabled);
    }
}