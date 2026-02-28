package com.example.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName;
    private FloatingActionButton sendMessageButton;
    private RecyclerView userTipsRecyclerView;
    private TipAdapter tipAdapter;
    private List<Tip> userTipList;

    private DatabaseReference usersReference;
    private DatabaseReference tipsReference;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        profileName = findViewById(R.id.profileName);
        sendMessageButton = findViewById(R.id.sendMessageButton);
        userTipsRecyclerView = findViewById(R.id.userTipsRecyclerView);

        userTipsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userTipList = new ArrayList<>();
        tipAdapter = new TipAdapter(userTipList);
        userTipsRecyclerView.setAdapter(tipAdapter);

        userId = getIntent().getStringExtra("userId");

        if (userId != null) {
            usersReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
            tipsReference = FirebaseDatabase.getInstance().getReference("tips");

            loadUserInfo();
            loadUserTips();
        }

        sendMessageButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void loadUserInfo() {
        usersReference.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.getValue(String.class);
                    profileName.setText(name);
                    getSupportActionBar().setTitle(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadUserTips() {
        Query userTipsQuery = tipsReference.orderByChild("userId").equalTo(userId);
        userTipsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userTipList.clear();
                for (DataSnapshot tipSnapshot : snapshot.getChildren()) {
                    Tip tip = tipSnapshot.getValue(Tip.class);
                    userTipList.add(tip);
                }
                tipAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load tips.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
