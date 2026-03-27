package com.example.travelbuddy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
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
 * - Mode AJOUT : formulaire vide → on crée un nouveau tip dans Firebase.
 * - Mode ÉDITION : les données existantes sont pré-remplies via l'Intent.
 * L'utilisateur peut aussi joindre une photo. L'image est compressée
 * puis stockée en Base64 directement dans la Realtime Database
 * (pas besoin de Firebase Storage ni de service payant).
 */
public class AddTipActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private TextInputEditText titleInput, descInput, locationInput;
    private MaterialButton postButton, selectImageButton;
    private ImageView imagePreview;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    // L'image sélectionnée, convertie en chaîne Base64 après compression
    private String imageBase64 = null;

    // Mode édition
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
        postButton = findViewById(R.id.postButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        imagePreview = findViewById(R.id.imagePreview);

        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        // Mode édition : on pré-remplit les champs
        if (getIntent().hasExtra("tipId")) {
            isEditMode = true;
            editTipId = getIntent().getStringExtra("tipId");
            titleInput.setText(getIntent().getStringExtra("tipTitle"));
            descInput.setText(getIntent().getStringExtra("tipDescription"));
            locationInput.setText(getIntent().getStringExtra("tipLocation"));
            getSupportActionBar().setTitle("Modifier le tip");
            postButton.setText("Enregistrer les modifications");
        }

        // Clic sur "Ajouter une photo" → ouvre la galerie du téléphone
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Choisir une photo"), PICK_IMAGE_REQUEST);
        });

        postButton.setOnClickListener(v -> {
            if (isEditMode) {
                updateTip();
            } else {
                postTip();
            }
        });
    }

    // L'utilisateur a choisi une image dans la galerie
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // On affiche l'aperçu
            imagePreview.setImageURI(imageUri);
            imagePreview.setVisibility(View.VISIBLE);
            selectImageButton.setText("Changer la photo");

            // On compresse l'image et on la convertit en Base64
            imageBase64 = compressAndEncode(imageUri);
        }
    }

    // Compresse l'image à une taille raisonnable puis la convertit en chaîne Base64.
    // On réduit la résolution et la qualité JPEG pour que ça tienne dans la BD
    // sans problème (on vise environ 50-100 Ko par image).
    private String compressAndEncode(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            // On redimensionne si l'image est trop grande (max 600px de large)
            int maxWidth = 600;
            Bitmap resized;
            if (original.getWidth() > maxWidth) {
                float ratio = (float) maxWidth / original.getWidth();
                int newHeight = Math.round(original.getHeight() * ratio);
                resized = Bitmap.createScaledBitmap(original, maxWidth, newHeight, true);
            } else {
                resized = original;
            }

            // Compression en JPEG qualité 60%
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] bytes = baos.toByteArray();

            // Conversion en Base64
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur image : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Crée un nouveau tip et l'enregistre dans Firebase
    private void postTip() {
        String id = databaseReference.push().getKey();
        long timestamp = System.currentTimeMillis();

        Tip tip = new Tip(
                id,
                titleInput.getText().toString(),
                descInput.getText().toString(),
                locationInput.getText().toString(),
                auth.getCurrentUser().getUid(),
                imageBase64,  // chaîne Base64 ou null si pas d'image
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

    // Met à jour un tip existant
    private void updateTip() {
        if (editTipId == null) return;

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("title", titleInput.getText().toString());
        updates.put("description", descInput.getText().toString());
        updates.put("location", locationInput.getText().toString());

        // Si une nouvelle image a été choisie, on la met aussi à jour
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
