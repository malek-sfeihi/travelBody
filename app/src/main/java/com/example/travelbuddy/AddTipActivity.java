package com.example.travelbuddy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

/*
 * Écran pour ajouter ou modifier une astuce de voyage.
 * L'utilisateur choisit un pays dans un dropdown, remplit titre/description/lieu,
 * peut joindre une photo (stockée en Base64) puis poste le tip.
 */
public class AddTipActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    // Liste des pays disponibles dans le dropdown
    private static final String[] COUNTRIES = {
            "Tunisie", "France", "Italie", "Espagne", "Maroc",
            "Turquie", "Allemagne", "Égypte", "Japon", "USA", "Autre"
    };

    private TextInputEditText titleInput, descInput, locationInput;
    private AutoCompleteTextView countryDropdown;
    private MaterialButton postButton, selectImageButton;
    private ImageView imagePreview;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    private String imageBase64 = null;
    private String editTipId = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tip);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        titleInput = findViewById(R.id.titleInput);
        descInput = findViewById(R.id.descInput);
        locationInput = findViewById(R.id.locationInput);
        countryDropdown = findViewById(R.id.countryDropdown);
        postButton = findViewById(R.id.postButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        imagePreview = findViewById(R.id.imagePreview);

        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        // On remplit le dropdown avec la liste des pays
        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, COUNTRIES);
        countryDropdown.setAdapter(countryAdapter);

        // Mode édition : pré-remplissage des champs
        if (getIntent().hasExtra("tipId")) {
            isEditMode = true;
            editTipId = getIntent().getStringExtra("tipId");
            titleInput.setText(getIntent().getStringExtra("tipTitle"));
            descInput.setText(getIntent().getStringExtra("tipDescription"));
            locationInput.setText(getIntent().getStringExtra("tipLocation"));

            // Pré-sélectionner le pays si disponible
            String editCountry = getIntent().getStringExtra("tipCountry");
            if (editCountry != null) {
                countryDropdown.setText(editCountry, false);
            }

            getSupportActionBar().setTitle("Modifier le tip");
            postButton.setText("Enregistrer les modifications");
        }

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Choisir une photo"), PICK_IMAGE_REQUEST);
        });

        postButton.setOnClickListener(v -> {
            // Vérification que le pays est sélectionné
            String country = countryDropdown.getText().toString().trim();
            if (country.isEmpty()) {
                countryDropdown.setError("Choisis un pays");
                return;
            }
            if (isEditMode) {
                updateTip();
            } else {
                postTip();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            imagePreview.setImageURI(imageUri);
            imagePreview.setVisibility(View.VISIBLE);
            selectImageButton.setText("Changer la photo");
            imageBase64 = compressAndEncode(imageUri);
        }
    }

    // Compresse l'image puis la convertit en Base64
    private String compressAndEncode(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            int maxWidth = 600;
            Bitmap resized;
            if (original.getWidth() > maxWidth) {
                float ratio = (float) maxWidth / original.getWidth();
                int newHeight = Math.round(original.getHeight() * ratio);
                resized = Bitmap.createScaledBitmap(original, maxWidth, newHeight, true);
            } else {
                resized = original;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur image : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Crée un nouveau tip avec le pays sélectionné
    private void postTip() {
        String id = databaseReference.push().getKey();
        long timestamp = System.currentTimeMillis();
        String country = countryDropdown.getText().toString().trim();

        Tip tip = new Tip(
                id,
                titleInput.getText().toString(),
                descInput.getText().toString(),
                locationInput.getText().toString(),
                country,
                auth.getCurrentUser().getUid(),
                imageBase64,
                timestamp
        );

        postButton.setEnabled(false);
        databaseReference.child(id).setValue(tip)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Tip posted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    postButton.setEnabled(true);
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Met à jour un tip existant (y compris le pays)
    private void updateTip() {
        if (editTipId == null) return;

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("title", titleInput.getText().toString());
        updates.put("description", descInput.getText().toString());
        updates.put("location", locationInput.getText().toString());
        updates.put("country", countryDropdown.getText().toString().trim());

        if (imageBase64 != null) {
            updates.put("imageUrl", imageBase64);
        }

        postButton.setEnabled(false);
        databaseReference.child(editTipId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Tip modifié", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    postButton.setEnabled(true);
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
