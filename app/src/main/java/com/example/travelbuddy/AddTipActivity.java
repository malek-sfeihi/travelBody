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

public class AddTipActivity extends AppCompatActivity {

    private TextInputEditText titleInput, descInput, locationInput;
    private MaterialButton postButton;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

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

        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        postButton.setOnClickListener(v -> {
            postTip(null);
        });
    }

    private void postTip(String imageUrl) {
        String id = databaseReference.push().getKey();
        long timestamp = System.currentTimeMillis();

        Tip tip = new Tip(
                id,
                titleInput.getText().toString(),
                descInput.getText().toString(),
                locationInput.getText().toString(),
                auth.getCurrentUser().getUid(),
                imageUrl,
                timestamp
        );

        databaseReference.child(id).setValue(tip)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Tip posted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
