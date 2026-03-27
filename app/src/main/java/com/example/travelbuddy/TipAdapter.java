package com.example.travelbuddy;

import android.content.Intent;
import android.text.format.DateUtils;
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

import de.hdodenhof.circleimageview.CircleImageView;

/*
 * Adapter pour afficher les astuces de voyage dans un RecyclerView.
 * Chaque item montre le titre, le lieu, la description, le nom de l'auteur
 * et depuis combien de temps le tip a été posté.
 * En cliquant sur le nom de l'auteur, on ouvre son profil.
 */
public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    private List<Tip> tipList;
    // Référence vers "users" pour aller chercher le nom de l'auteur de chaque tip
    private DatabaseReference usersReference;

    public TipAdapter(List<Tip> tipList) {
        this.tipList = tipList;
        this.usersReference = FirebaseDatabase.getInstance().getReference("users");
    }

    // Appelé quand le RecyclerView a besoin d'une nouvelle cellule
    // On inflate le layout item_tip.xml
    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tip, parent, false);
        return new TipViewHolder(view);
    }

    // Appelé pour remplir une cellule avec les données d'un tip
    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        Tip tip = tipList.get(position);
        holder.title.setText(tip.title);
        holder.location.setText(tip.location);

        // On affiche la description seulement si elle n'est pas vide
        if (tip.description != null && !tip.description.trim().isEmpty()) {
            holder.description.setText(tip.description);
            holder.description.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.description.setVisibility(android.view.View.GONE);
        }

        // Calcul du temps relatif ("il y a 2 heures", "hier", etc.)
        long now = System.currentTimeMillis();
        CharSequence ago = DateUtils.getRelativeTimeSpanString(tip.timestamp, now, DateUtils.MINUTE_IN_MILLIS);
        holder.timestamp.setText(ago);

        // On récupère le nom de l'auteur depuis Firebase (pas stocké dans le tip lui-même)
        if (tip.userId != null) {
            usersReference.child(tip.userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String authorName = snapshot.getValue(String.class);
                        holder.author.setText(authorName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        // Clic sur le nom de l'auteur → on ouvre son profil
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

    // ViewHolder : garde les références des vues d'un item pour éviter de les chercher à chaque fois
    static class TipViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, author, timestamp, description;
        CircleImageView authorProfileImage;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tipTitle);
            location = itemView.findViewById(R.id.tipLocation);
            author = itemView.findViewById(R.id.tipAuthor);
            timestamp = itemView.findViewById(R.id.tipTimestamp);
            description = itemView.findViewById(R.id.tipDescription);
            authorProfileImage = itemView.findViewById(R.id.authorProfileImage);
        }
    }
}
