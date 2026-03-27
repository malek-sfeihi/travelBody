package com.example.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/*
 * Écran principal après connexion : le fil d'astuces de voyage.
 * Affiche un header de bienvenue personnalisé avec le nom de l'utilisateur
 * puis la liste des tips dans un RecyclerView avec animation.
 */
public class TipsFeedActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<Tip> tipList;
    TipAdapter adapter;
    DatabaseReference databaseReference;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips_feed);

        setSupportActionBar(findViewById(R.id.toolbar));

        // RecyclerView avec layout vertical
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tipList = new ArrayList<>();
        adapter = new TipAdapter(tipList);
        recyclerView.setAdapter(adapter);

        // Animation slide-in pour les items du RecyclerView
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
                this, R.anim.layout_animation_slide_in);
        recyclerView.setLayoutAnimation(animation);

        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        // Personnalisation de l'en-tête de bienvenue avec le nom de l'utilisateur
        loadWelcomeHeader();

        // Navigation du bas
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_map) {
                startActivity(new Intent(this, MapsActivity.class));
                return true;
            } else if (itemId == R.id.navigation_add) {
                startActivity(new Intent(this, AddTipActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                startActivity(new Intent(this, ConversationsActivity.class));
                return true;
            } else if (itemId == R.id.navigation_logout) {
                auth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Écoute en temps réel des tips
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tipList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Tip tip = data.getValue(Tip.class);
                    tipList.add(tip);
                }
                adapter.notifyDataSetChanged();
                // Relancer l'animation quand les données changent
                recyclerView.scheduleLayoutAnimation();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TipsFeedActivity.this,
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Charge le nom de l'utilisateur connecté et met à jour le texte de bienvenue
    private void loadWelcomeHeader() {
        FirebaseUser user = auth.getCurrentUser();
        TextView welcomeText = findViewById(R.id.welcomeText);

        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(user.getUid()).child("name");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.getValue(String.class);
                        welcomeText.setText("Bonjour, " + name + " !");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }
}
