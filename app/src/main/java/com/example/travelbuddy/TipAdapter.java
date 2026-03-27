package com.example.travelbuddy;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/*
 * Adapter pour afficher les astuces de voyage dans un RecyclerView.
 * Gère l'affichage du tip (titre, photo, lieu, auteur, avatar),
 * les boutons modifier/supprimer (si c'est mon tip),
 * et le système de likes (coeur).
 */
public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    private List<Tip> tipList;
    private DatabaseReference usersReference;
    private DatabaseReference tipsReference;
    private String currentUserId;

    public TipAdapter(List<Tip> tipList) {
        this.tipList = tipList;
        this.usersReference = FirebaseDatabase.getInstance().getReference("users");
        this.tipsReference = FirebaseDatabase.getInstance().getReference("tips");
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
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

        // Affiche la description si elle n'est pas vide
        if (tip.description != null && !tip.description.trim().isEmpty()) {
            holder.description.setText(tip.description);
            holder.description.setVisibility(View.VISIBLE);
        } else {
            holder.description.setVisibility(View.GONE);
        }

        // Temps relatif
        long now = System.currentTimeMillis();
        CharSequence ago = DateUtils.getRelativeTimeSpanString(tip.timestamp, now, DateUtils.MINUTE_IN_MILLIS);
        holder.timestamp.setText(ago);

        // Photo du tip en Base64
        if (tip.imageUrl != null && !tip.imageUrl.trim().isEmpty()) {
            holder.tipImage.setVisibility(View.VISIBLE);
            try {
                byte[] decodedBytes = Base64.decode(tip.imageUrl, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.tipImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.tipImage.setVisibility(View.GONE);
            }
        } else {
            holder.tipImage.setVisibility(View.GONE);
        }

        // Nom de l'auteur + avatar
        if (tip.userId != null) {
            usersReference.child(tip.userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String authorName = snapshot.getValue(String.class);
                        holder.author.setText(authorName);
                        String avatarUrl = "https://ui-avatars.com/api/?name="
                                + authorName.replace(" ", "+")
                                + "&background=random&color=fff&size=128";
                        Glide.with(holder.itemView.getContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.default_profile_background)
                                .into(holder.authorProfileImage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        // ===== SYSTÈME DE LIKES =====
        // On écoute le noeud likes de ce tip pour mettre à jour en temps réel
        if (tip.id != null) {
            DatabaseReference likesRef = tipsReference.child(tip.id).child("likes");

            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long count = snapshot.getChildrenCount();
                    // Affiche le nombre de likes (ou rien si 0)
                    holder.likeCount.setText(count > 0 ? String.valueOf(count) : "");

                    // On vérifie si MOI j'ai déjà liké ce tip
                    boolean iLiked = snapshot.hasChild(currentUserId != null ? currentUserId : "");
                    // Coeur plein si liké, vide sinon
                    holder.btnLike.setImageResource(
                            iLiked ? android.R.drawable.btn_star_big_on
                                    : android.R.drawable.btn_star_big_off);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

            // Clic sur le coeur → toggle like/unlike
            holder.btnLike.setOnClickListener(v -> {
                if (currentUserId == null) return;
                DatabaseReference myLikeRef = likesRef.child(currentUserId);

                // On vérifie si j'ai déjà liké
                myLikeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Déjà liké → on retire le like
                            myLikeRef.removeValue();
                        } else {
                            // Pas encore liké → on ajoute
                            myLikeRef.setValue(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            });
        }

        // Boutons modifier/supprimer visibles seulement pour mes propres tips
        if (currentUserId != null && currentUserId.equals(tip.userId)) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), AddTipActivity.class);
                intent.putExtra("tipId", tip.id);
                intent.putExtra("tipTitle", tip.title);
                intent.putExtra("tipDescription", tip.description);
                intent.putExtra("tipLocation", tip.location);
                intent.putExtra("tipCountry", tip.country);
                holder.itemView.getContext().startActivity(intent);
            });

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("Supprimer")
                        .setMessage("Tu veux vraiment supprimer cette astuce ?")
                        .setPositiveButton("Oui", (dialog, which) -> {
                            if (tip.id != null) {
                                tipsReference.child(tip.id).removeValue()
                                        .addOnSuccessListener(unused ->
                                                Toast.makeText(holder.itemView.getContext(),
                                                        "Tip supprimé", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e ->
                                                Toast.makeText(holder.itemView.getContext(),
                                                        "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Non", null)
                        .show();
            });
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Clic sur le nom de l'auteur → profil
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
        TextView title, location, author, timestamp, description;
        TextView likeCount;
        CircleImageView authorProfileImage;
        ImageView btnDelete, btnEdit, tipImage;
        ImageView btnLike;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tipTitle);
            location = itemView.findViewById(R.id.tipLocation);
            author = itemView.findViewById(R.id.tipAuthor);
            timestamp = itemView.findViewById(R.id.tipTimestamp);
            description = itemView.findViewById(R.id.tipDescription);
            authorProfileImage = itemView.findViewById(R.id.authorProfileImage);
            btnDelete = itemView.findViewById(R.id.btnDeleteTip);
            btnEdit = itemView.findViewById(R.id.btnEditTip);
            tipImage = itemView.findViewById(R.id.tipImage);
            btnLike = itemView.findViewById(R.id.btnLike);
            likeCount = itemView.findViewById(R.id.likeCount);
        }
    }
}
