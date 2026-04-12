package com.example.tradget;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ChatFragment extends Fragment {

    EditText chatInput;
    ImageView sendButton;
    LinearLayout chatMessagesContainer;
    ScrollView chatScrollView;
    View view;

    public ChatFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatInput = view.findViewById(R.id.chatInput);
        sendButton = view.findViewById(R.id.sendButton);
        chatMessagesContainer = view.findViewById(R.id.chatMessagesContainer);
        chatScrollView = view.findViewById(R.id.chatScrollView);

        sendButton.setOnClickListener(v -> sendMessage());

        return view;
    }

    private void sendMessage() {
        String message = chatInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(getContext(), "Please type a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add sent message to chat
        addMessage(message, true);
        chatInput.setText("");

        // Simulate reply after 1 second
        chatScrollView.postDelayed(() -> {
            addMessage("Thanks for the update! See you soon.", false);
        }, 1000);
    }

    private void addMessage(String text, boolean isSent) {
        LinearLayout messageContainer = new LinearLayout(getContext());
        messageContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(12, 12, 12, 12);

        if (isSent) {
            messageContainer.setBackgroundColor(0xFF00BCD4);
            ((LinearLayout.LayoutParams) messageContainer.getLayoutParams()).gravity = android.view.Gravity.END;
            messageContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 60f));
        } else {
            messageContainer.setBackgroundColor(0xFFFFFFFF);
            ((LinearLayout.LayoutParams) messageContainer.getLayoutParams()).gravity = android.view.Gravity.START;
            messageContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 60f));
        }

        TextView messageText = new TextView(getContext());
        messageText.setText(text);
        messageText.setTextSize(14);
        messageText.setTextColor(isSent ? 0xFFFFFFFF : 0xFF212121);

        TextView timeText = new TextView(getContext());
        timeText.setText("10:35 AM");
        timeText.setTextSize(10);
        timeText.setTextColor(isSent ? 0x80FFFFFF : 0xFF9E9E9E);
        timeText.setGravity(android.view.Gravity.END);
        timeText.setPadding(0, 4, 0, 0);

        messageContainer.addView(messageText);
        messageContainer.addView(timeText);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (isSent) {
            params.gravity = android.view.Gravity.END;
            params.setMargins(60, 0, 0, 12);
        } else {
            params.gravity = android.view.Gravity.START;
            params.setMargins(0, 0, 60, 12);
        }
        messageContainer.setLayoutParams(params);

        chatMessagesContainer.addView(messageContainer);
        chatScrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }
}