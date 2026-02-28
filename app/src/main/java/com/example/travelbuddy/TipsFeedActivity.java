package com.example.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/*
 * TipsFeedActivity :
 * Ecran qui affichera plus tard la liste des conseils de voyage.
 */
public class TipsFeedActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<Tip> tipList;
    TipAdapter adapter;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips_feed);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tipList = new ArrayList<>();
        adapter = new TipAdapter(tipList);
        recyclerView.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("tips");

        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddTipActivity.class)));

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tipList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Tip tip = data.getValue(Tip.class);
                    tipList.add(tip);
                }
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