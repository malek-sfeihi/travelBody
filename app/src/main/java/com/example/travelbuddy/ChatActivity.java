package com.example.travelbuddy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private String receiverId;
    private String senderId;
    private String chatRoomId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("userId");
        senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(messageAdapter);

        createChatRoomId();
        loadMessages();

        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });
    }

    private void createChatRoomId() {
        if (senderId.compareTo(receiverId) > 0) {
            chatRoomId = senderId + receiverId;
        } else {
            chatRoomId = receiverId + senderId;
        }
    }

    private void loadMessages() {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatRoomId).child("messages");
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Message message = data.getValue(Message.class);
                    messageList.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String messageText) {
        long timestamp = System.currentTimeMillis();
        Message message = new Message(messageText, senderId, receiverId, timestamp);

        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatRoomId).child("messages");
        messagesRef.push().setValue(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText("");
            }
        });
    }
}
