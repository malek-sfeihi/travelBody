package com.example.travelbuddy;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/*
 * Écran pour ajouter une nouvelle astuce de voyage.
 * L'utilisateur remplit un titre, une description et un lieu,
 * puis on enregistre le tout dans Firebase sous "tips/{id}".
 */
public class AddTipActivity extends AppCompatActivity {

    // Champs de saisie du formulaire
    private TextInputEditText titleInput, descInput, locationInput;
    private MaterialButton postButton;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tip);

        // Toolbar avec bouton retour
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Récupération des champs de saisie
        titleInput = findViewById(R.id.titleInput);
        descInput = findViewById(R.id.descInput);
        locationInput = findViewById(R.id.locationInput);
        postButton = findViewById(R.id.postButton);

        // Référence vers le noeud "tips" dans Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        // Quand on clique sur "Poster", on envoie le tip
        postButton.setOnClickListener(v -> {
            postTip(null);
        });
    }

    // Crée un objet Tip et l'enregistre dans Firebase
    private void postTip(String imageUrl) {
        // push() génère un id unique automatiquement
        String id = databaseReference.push().getKey();
        long timestamp = System.currentTimeMillis();

        // On crée l'objet Tip avec toutes les infos
        Tip tip = new Tip(
                id,
                titleInput.getText().toString(),
                descInput.getText().toString(),
                locationInput.getText().toString(),
                auth.getCurrentUser().getUid(),  // on associe le tip à l'utilisateur connecté
                imageUrl,
                timestamp
        );

        // Sauvegarde dans Firebase : tips/{id} → objet tip
        databaseReference.child(id).setValue(tip)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Tip posted", Toast.LENGTH_SHORT).show();
                    finish(); // retour à l'écran précédent
                });
    }

    // Bouton retour dans la toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
