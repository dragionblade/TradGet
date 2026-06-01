package com.example.tradget;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.tradget.model.RideRequest;

import java.util.List;

/**
 * Shows the list of active (ACCEPTED) chat conversations for the current user.
 * Tapping a conversation opens {@link ChatFragment} for that chat.
 */
public class ChatListFragment extends Fragment {

    private LinearLayout chatsContainer;
    private TextView     emptyLabel;
    private ScrollView   chatsScroll;

    private SessionManager     session;
    private FirestoreRepository repo;

    public ChatListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        chatsContainer = view.findViewById(R.id.chatsContainer);
        emptyLabel     = view.findViewById(R.id.emptyChatLabel);
        chatsScroll    = view.findViewById(R.id.chatsScroll);

        session = new SessionManager(requireContext());
        repo    = FirestoreRepository.getInstance();

        loadChats();
        return view;
    }

    private void loadChats() {
        repo.getAcceptedRequestsForUser(session.getUid(), new FirestoreRepository.Callback<List<RideRequest>>() {
            @Override
            public void onSuccess(List<RideRequest> requests) {
                chatsContainer.removeAllViews();
                if (requests.isEmpty()) {
                    emptyLabel.setVisibility(View.VISIBLE);
                    chatsScroll.setVisibility(View.GONE);
                    return;
                }
                emptyLabel.setVisibility(View.GONE);
                chatsScroll.setVisibility(View.VISIBLE);

                for (RideRequest req : requests) {
                    addChatItem(req);
                }
            }
            @Override
            public void onFailure(String error) {
                emptyLabel.setText("Could not load chats");
                emptyLabel.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addChatItem(RideRequest req) {
        String uid = session.getUid();
        // Determine who the other person is
        boolean isPassenger = uid.equals(req.getPassengerId());
        String otherName    = isPassenger ? req.getRiderName() : req.getPassengerName();
        String chatId       = req.getChatId();
        String routeSummary = req.getPickupName() + " → " + req.getDropName();

        LinearLayout item = new LinearLayout(requireContext());
        item.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(20, 20, 20, 20);
        item.setBackgroundResource(android.R.drawable.list_selector_background);
        item.setClickable(true);
        item.setFocusable(true);

        // Avatar
        LinearLayout avatar = new LinearLayout(requireContext());
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(52, 52);
        avatar.setLayoutParams(ap);
        avatar.setBackgroundResource(R.drawable.bg_toggle_selected);
        avatar.setGravity(android.view.Gravity.CENTER);

        TextView initial = new TextView(requireContext());
        initial.setText(otherName != null && !otherName.isEmpty()
                ? String.valueOf(otherName.charAt(0)).toUpperCase() : "?");
        initial.setTextSize(20);
        initial.setTextColor(0xFFFFFFFF);
        initial.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.addView(initial);

        // Text block
        LinearLayout textBlock = new LinearLayout(requireContext());
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMarginStart(16);
        textBlock.setLayoutParams(tp);
        textBlock.setOrientation(LinearLayout.VERTICAL);

        TextView nameView = new TextView(requireContext());
        nameView.setText(otherName != null ? otherName : "Unknown");
        nameView.setTextSize(16);
        nameView.setTextColor(0xFF212121);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView routeView = new TextView(requireContext());
        routeView.setText(routeSummary);
        routeView.setTextSize(12);
        routeView.setTextColor(0xFF757575);
        routeView.setPadding(0, 2, 0, 0);

        textBlock.addView(nameView);
        textBlock.addView(routeView);

        item.addView(avatar);
        item.addView(textBlock);

        // Divider
        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFE0E0E0);

        String finalOtherName = otherName;
        item.setOnClickListener(v -> openChat(req, finalOtherName));

        chatsContainer.addView(item);
        chatsContainer.addView(divider);
    }

    private void openChat(RideRequest req, String otherName) {
        Bundle args = new Bundle();
        args.putString("chat_id",   req.getChatId());
        args.putString("chat_name", otherName);
        args.putString("request_id", req.getRequestId());
        args.putString("ride_id", req.getRideId());
        args.putString("rider_id", req.getRiderId());
        args.putString("passenger_id", req.getPassengerId());
        args.putDouble("cost_agreed", req.getCostAgreed());

        ChatFragment chatFragment = new ChatFragment();
        chatFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, chatFragment)
            .addToBackStack(null)
            .commit();
    }
}
