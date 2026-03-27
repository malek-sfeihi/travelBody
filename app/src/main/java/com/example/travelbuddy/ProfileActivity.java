package com.example.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/*
 * Écran profil d'un utilisateur.
 * Accessible quand on clique sur le nom d'un auteur dans le fil d'astuces.
 * On affiche son nom, ses tips, et un bouton pour lui envoyer un message.
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView profileName;
    private FloatingActionButton sendMessageButton; // bouton flottant pour envoyer un message
    private RecyclerView userTipsRecyclerView;
    private TipAdapter tipAdapter;
    private List<Tip> userTipList;   // liste des tips postés par cet utilisateur

    private DatabaseReference usersReference;
    private DatabaseReference tipsReference;
    private String userId;   // uid de l'utilisateur dont on consulte le profil

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Toolbar avec bouton retour
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialisation des vues
        profileName = findViewById(R.id.profileName);
        sendMessageButton = findViewById(R.id.sendMessageButton);
        userTipsRecyclerView = findViewById(R.id.userTipsRecyclerView);

        // RecyclerView pour la liste des tips de cet utilisateur
        userTipsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userTipList = new ArrayList<>();
        tipAdapter = new TipAdapter(userTipList);
        userTipsRecyclerView.setAdapter(tipAdapter);

        // On récupère l'userId passé par l'activité précédente (via Intent)
        userId = getIntent().getStringExtra("userId");

        if (userId != null) {
            usersReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
            tipsReference = FirebaseDatabase.getInstance().getReference("tips");

            loadUserInfo();
            loadUserTips();
        }

        // Clic sur le bouton message → ouvre le chat avec cet utilisateur
        sendMessageButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    // Charge le nom de l'utilisateur depuis Firebase et l'affiche
    private void loadUserInfo() {
        usersReference.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.getValue(String.class);
                    profileName.setText(name);
                    getSupportActionBar().setTitle(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // Charge uniquement les tips postés par cet utilisateur
    // On utilise une Query Firebase : orderByChild("userId").equalTo(userId)
    private void loadUserTips() {
        Query userTipsQuery = tipsReference.orderByChild("userId").equalTo(userId);
        userTipsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userTipList.clear();
                for (DataSnapshot tipSnapshot : snapshot.getChildren()) {
                    Tip tip = tipSnapshot.getValue(Tip.class);
                    userTipList.add(tip);
                }
                tipAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load tips.", Toast.LENGTH_SHORT).show();
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
