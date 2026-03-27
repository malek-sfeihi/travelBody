package com.example.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/*
 * Écran principal après connexion : le fil d'astuces de voyage.
 * On affiche tous les tips postés par les utilisateurs dans un RecyclerView.
 * La barre de navigation en bas permet d'accéder à la carte, ajouter un tip,
 * voir les messages ou se déconnecter.
 */
public class TipsFeedActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<Tip> tipList;         // liste qui contient tous les tips récupérés de Firebase
    TipAdapter adapter;        // adapter qui fait le lien entre tipList et le RecyclerView
    DatabaseReference databaseReference;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips_feed);

        setSupportActionBar(findViewById(R.id.toolbar));

        // Mise en place du RecyclerView avec un layout vertical classique
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // On initialise la liste vide et on branche l'adapter
        tipList = new ArrayList<>();
        adapter = new TipAdapter(tipList);
        recyclerView.setAdapter(adapter);

        // Référence vers le noeud "tips" dans Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        // Gestion de la barre de navigation du bas
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_map) {
                // Ouvre la carte avec les marqueurs
                startActivity(new Intent(this, MapsActivity.class));
                return true;
            } else if (itemId == R.id.navigation_add) {
                // Ouvre le formulaire pour poster un nouveau tip
                startActivity(new Intent(this, AddTipActivity.class));
                return true;
            } else if (itemId == R.id.navigation_messages) {
                // Ouvre la liste des conversations
                startActivity(new Intent(this, ConversationsActivity.class));
                return true;
            } else if (itemId == R.id.navigation_logout) {
                // Déconnexion : on vide la pile d'activités et on retourne au login
                auth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Écoute en temps réel sur le noeud "tips"
        // Dès qu'un tip est ajouté/modifié/supprimé, la liste se met à jour toute seule
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tipList.clear(); // on vide d'abord pour éviter les doublons
                for (DataSnapshot data : snapshot.getChildren()) {
                    // Chaque enfant du noeud "tips" est converti en objet Tip
                    Tip tip = data.getValue(Tip.class);
                    tipList.add(tip);
                }
                // On notifie l'adapter que les données ont changé
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TipsFeedActivity.this,
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
