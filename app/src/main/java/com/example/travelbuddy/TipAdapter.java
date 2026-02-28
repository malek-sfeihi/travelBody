package com.example.travelbuddy;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    private List<Tip> tipList;
    private DatabaseReference usersReference;

    public TipAdapter(List<Tip> tipList) {
        this.tipList = tipList;
        this.usersReference = FirebaseDatabase.getInstance().getReference("users");
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tip, parent, false);
        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        Tip tip = tipList.get(position);
        holder.title.setText(tip.title);
        holder.location.setText(tip.location);

        if (tip.userId != null) {
            usersReference.child(tip.userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String authorName = snapshot.getValue(String.class);
                        holder.author.setText("by " + authorName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        holder.author.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ProfileActivity.class);
            intent.putExtra("userId", tip.userId);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tipList.size();
    }

    static class TipViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, author;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tipTitle);
            location = itemView.findViewById(R.id.tipLocation);
            author = itemView.findViewById(R.id.tipAuthor);
        }
    }
}
