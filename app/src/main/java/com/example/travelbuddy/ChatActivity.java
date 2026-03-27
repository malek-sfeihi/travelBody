package com.example.travelbuddy;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/*
 * Écran de chat entre deux utilisateurs.
 * Les messages sont stockés dans Firebase sous "chats/{chatRoomId}/messages/".
 * Le chatRoomId est généré en concaténant les deux UIDs dans un ordre fixe
 * pour que les deux utilisateurs tombent toujours sur le même salon.
 */
public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private TextInputEditText messageInput;
    private MaterialButton sendButton;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private String receiverId;   // uid de la personne à qui on parle
    private String senderId;     // mon uid
    private String chatRoomId;   // clé unique du salon de chat

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Toolbar avec bouton retour
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // On récupère l'id du destinataire passé par l'activité précédente
        receiverId = getIntent().getStringExtra("userId");
        senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialisation des vues
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Configuration du RecyclerView pour afficher les messages
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(messageAdapter);

        // On génère l'identifiant du salon et on charge les messages
        createChatRoomId();
        loadMessages();

        // Envoi d'un message quand on clique sur le bouton
        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });
    }

    // Génère un identifiant unique pour le salon de chat.
    // Astuce : on trie les deux UIDs par ordre alphabétique et on les concatène.
    // Comme ça, peu importe qui ouvre le chat en premier, on obtient la même clé.
    private void createChatRoomId() {
        if (senderId.compareTo(receiverId) > 0) {
            chatRoomId = senderId + receiverId;
        } else {
            chatRoomId = receiverId + senderId;
        }
    }

    // Écoute en temps réel les messages du salon
    // Dès qu'un nouveau message arrive, la liste se met à jour automatiquement
    private void loadMessages() {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatRoomId).child("messages");

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Message message = data.getValue(Message.class);
                    messageList.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                // On scrolle vers le dernier message pour que ce soit visible
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Envoie un message : on crée un objet Message et on le push dans Firebase
    private void sendMessage(String messageText) {
        long timestamp = System.currentTimeMillis();
        Message message = new Message(messageText, senderId, receiverId, timestamp);

        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatRoomId).child("messages");

        // push() génère une clé unique pour chaque message
        messagesRef.push().setValue(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                messageInput.setText(""); // on vide le champ après envoi
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
