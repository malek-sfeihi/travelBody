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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*
 * Écran principal : fil d'astuces de voyage avec filtres par pays.
 * Les chips en haut permettent de filtrer les tips par pays.
 * "Tous" affiche tous les tips, les autres chips filtrent par pays.
 */
public class TipsFeedActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<Tip> allTips;          // tous les tips depuis Firebase
    List<Tip> filteredTips;     // tips filtrés affichés dans le RecyclerView
    TipAdapter adapter;
    DatabaseReference databaseReference;
    FirebaseAuth auth;

    ChipGroup chipGroup;
    SwipeRefreshLayout swipeRefresh;
    String selectedCountry = null; // null = "Tous"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips_feed);

        setSupportActionBar(findViewById(R.id.toolbar));

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chipGroup = findViewById(R.id.chipGroup);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // Couleur du cercle de chargement = bleu de l'app
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);

        allTips = new ArrayList<>();
        filteredTips = new ArrayList<>();
        adapter = new TipAdapter(filteredTips);
        recyclerView.setAdapter(adapter);

        // Animation slide-in
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(
                this, R.anim.layout_animation_slide_in);
        recyclerView.setLayoutAnimation(animation);

        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        loadWelcomeHeader();

        // Pull-to-refresh : tire vers le bas → recharge les tips depuis Firebase
        swipeRefresh.setOnRefreshListener(() -> {
            databaseReference.get().addOnCompleteListener(task -> {
                swipeRefresh.setRefreshing(false);
            });
        });

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
                allTips.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Tip tip = data.getValue(Tip.class);
                    allTips.add(tip);
                }
                // On reconstruit les chips avec les pays trouvés dans les données
                buildCountryChips();
                // On applique le filtre actuel
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TipsFeedActivity.this,
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Construit les chips dynamiquement à partir des pays présents dans les tips
    private void buildCountryChips() {
        chipGroup.removeAllViews();

        // On récupère les pays uniques tout en gardant l'ordre d'apparition
        Set<String> countries = new LinkedHashSet<>();
        for (Tip tip : allTips) {
            if (tip.country != null && !tip.country.trim().isEmpty()) {
                countries.add(tip.country);
            }
        }

        // Chip "Tous" toujours en premier
        Chip allChip = new Chip(this);
        allChip.setText("Tous");
        allChip.setCheckable(true);
        allChip.setChecked(selectedCountry == null);
        allChip.setOnClickListener(v -> {
            selectedCountry = null;
            applyFilter();
        });
        chipGroup.addView(allChip);

        // Un chip par pays trouvé
        for (String country : countries) {
            Chip chip = new Chip(this);
            chip.setText(country);
            chip.setCheckable(true);
            chip.setChecked(country.equals(selectedCountry));
            chip.setOnClickListener(v -> {
                selectedCountry = country;
                applyFilter();
            });
            chipGroup.addView(chip);
        }
    }

    // Filtre les tips selon le pays sélectionné et met à jour le RecyclerView
    private void applyFilter() {
        filteredTips.clear();

        if (selectedCountry == null) {
            // "Tous" est sélectionné → on affiche tout
            filteredTips.addAll(allTips);
        } else {
            // On ne garde que les tips du pays choisi
            for (Tip tip : allTips) {
                if (selectedCountry.equals(tip.country)) {
                    filteredTips.add(tip);
                }
            }
        }

        adapter.notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    // Charge le nom de l'utilisateur et met à jour le texte de bienvenue
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
