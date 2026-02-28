package com.example.travelbuddy;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddTipActivity extends AppCompatActivity {

    TextInputEditText titleInput, descInput, locationInput;
    MaterialButton postButton;

    DatabaseReference databaseReference;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tip);

        titleInput = findViewById(R.id.titleInput);
        descInput = findViewById(R.id.descInput);
        locationInput = findViewById(R.id.locationInput);
        postButton = findViewById(R.id.postButton);

        databaseReference = FirebaseDatabase.getInstance().getReference("tips");
        auth = FirebaseAuth.getInstance();

        postButton.setOnClickListener(v -> {

            String id = databaseReference.push().getKey();

            Tip tip = new Tip(
                    id,
                    titleInput.getText().toString(),
                    descInput.getText().toString(),
                    locationInput.getText().toString(),
                    auth.getCurrentUser().getUid()
            );

            databaseReference.child(id).setValue(tip)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Tip posted", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        });
    }
}
