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

import java.util.HashMap;

/*
 * Écran pour ajouter ou modifier une astuce de voyage.
 * - Mode AJOUT : l'utilisateur remplit un formulaire vide → on crée un nouveau tip dans Firebase.
 * - Mode ÉDITION : on reçoit les données du tip existant via l'Intent,
 *   le formulaire est pré-rempli, et on met à jour le tip au lieu d'en créer un nouveau.
 */
public class AddTipActivity extends AppCompatActivity {

    // Champs de saisie du formulaire
    private TextInputEditText titleInput, descInput, locationInput;
    private MaterialButton postButton;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    // Si on est en mode édition, on stocke l'id du tip à modifier
    private String editTipId = null;
    private boolean isEditMode = false;

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

        // On vérifie si on est en mode édition (l'Intent contient un tipId)
        if (getIntent().hasExtra("tipId")) {
            isEditMode = true;
            editTipId = getIntent().getStringExtra("tipId");

            // On pré-remplit les champs avec les données existantes
            titleInput.setText(getIntent().getStringExtra("tipTitle"));
            descInput.setText(getIntent().getStringExtra("tipDescription"));
            locationInput.setText(getIntent().getStringExtra("tipLocation"));

            // On change le titre et le texte du bouton pour indiquer qu'on modifie
            getSupportActionBar().setTitle("Modifier le tip");
            postButton.setText("Enregistrer les modifications");
        }

        // Clic sur le bouton : soit on crée, soit on met à jour
        postButton.setOnClickListener(v -> {
            if (isEditMode) {
                updateTip();
            } else {
                postTip(null);
            }
        });
    }

    // Crée un nouveau tip et l'enregistre dans Firebase
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
                auth.getCurrentUser().getUid(),
                imageUrl,
                timestamp
        );

        // Sauvegarde dans Firebase : tips/{id} → objet tip
        databaseReference.child(id).setValue(tip)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Tip posted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // Met à jour un tip existant dans Firebase (seulement les champs modifiés)
    private void updateTip() {
        if (editTipId == null) return;

        // On prépare un HashMap avec les nouvelles valeurs
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("title", titleInput.getText().toString());
        updates.put("description", descInput.getText().toString());
        updates.put("location", locationInput.getText().toString());

        // updateChildren() ne touche qu'aux champs spécifiés, le reste ne change pas
        databaseReference.child(editTipId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Tip modifié", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Bouton retour dans la toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
