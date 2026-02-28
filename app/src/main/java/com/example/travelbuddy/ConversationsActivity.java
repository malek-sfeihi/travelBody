package com.example.travelbuddy;

import android.os.Bundle;
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
import java.util.List;

public class ConversationsActivity extends AppCompatActivity {

    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;

    private DatabaseReference chatsRef;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        conversationsRecyclerView = findViewById(R.id.conversationsRecyclerView);
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationList);
        conversationsRecyclerView.setAdapter(conversationAdapter);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadConversations();
    }

    private void loadConversations() {
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversationList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatRoomId = chatSnapshot.getKey();
                    if (chatRoomId != null && chatRoomId.contains(currentUserId)) {
                        String otherUserId = chatRoomId.replace(currentUserId, "");

                        usersRef.child(otherUserId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    String otherUserName = userSnapshot.getValue(String.class);
                                    Conversation conversation = new Conversation(otherUserId, otherUserName);
                                    conversationList.add(conversation);
                                    conversationAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ConversationsActivity.this, "Failed to load conversations.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
