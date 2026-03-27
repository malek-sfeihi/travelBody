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

/*
 * Écran qui affiche la liste des conversations de l'utilisateur connecté.
 * On parcourt le noeud "chats" dans Firebase : chaque salon a une clé
 * composée des deux UIDs concaténés. Si la clé contient mon UID,
 * c'est que je fais partie de cette conversation.
 */
public class ConversationsActivity extends AppCompatActivity {

    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;

    private DatabaseReference chatsRef;   // référence vers "chats"
    private DatabaseReference usersRef;   // référence vers "users" (pour récupérer les noms)
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        // Toolbar avec bouton retour
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Mise en place du RecyclerView
        conversationsRecyclerView = findViewById(R.id.conversationsRecyclerView);
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationList);
        conversationsRecyclerView.setAdapter(conversationAdapter);

        // On récupère l'UID de l'utilisateur connecté
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadConversations();
    }

    // Charge toutes les conversations où l'utilisateur courant est impliqué
    private void loadConversations() {
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversationList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String chatRoomId = chatSnapshot.getKey();

                    // Si la clé du salon contient mon UID, c'est ma conversation
                    if (chatRoomId != null && chatRoomId.contains(currentUserId)) {
                        // On déduit l'UID de l'autre personne en enlevant le mien
                        String otherUserId = chatRoomId.replace(currentUserId, "");

                        // On va chercher le nom de l'autre utilisateur dans "users"
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

    // Bouton retour dans la toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
