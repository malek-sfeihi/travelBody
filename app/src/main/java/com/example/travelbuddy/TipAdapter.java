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
 * Chaque item montre le titre, le lieu, la description, la photo (si disponible),
 * le nom de l'auteur avec son avatar et depuis combien de temps le tip a été posté.
 * Si le tip appartient à l'utilisateur connecté, on affiche les icones modifier/supprimer.
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

        // Temps relatif ("il y a 2 heures", etc.)
        long now = System.currentTimeMillis();
        CharSequence ago = DateUtils.getRelativeTimeSpanString(tip.timestamp, now, DateUtils.MINUTE_IN_MILLIS);
        holder.timestamp.setText(ago);

        // Si le tip a une photo (stockée en Base64 dans la BD), on la décode et on l'affiche
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

        // On récupère le nom de l'auteur et on charge son avatar
        if (tip.userId != null) {
            usersReference.child(tip.userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String authorName = snapshot.getValue(String.class);
                        holder.author.setText(authorName);

                        // Avatar généré automatiquement via UI Avatars (gratuit, sans clé API)
                        // On utilise le nom de l'auteur comme seed → chaque user a un avatar unique
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

        // Boutons modifier/supprimer visibles seulement pour mes propres tips
        if (currentUserId != null && currentUserId.equals(tip.userId)) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            // Crayon → ouvre le formulaire pré-rempli pour modifier
            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), AddTipActivity.class);
                intent.putExtra("tipId", tip.id);
                intent.putExtra("tipTitle", tip.title);
                intent.putExtra("tipDescription", tip.description);
                intent.putExtra("tipLocation", tip.location);
                holder.itemView.getContext().startActivity(intent);
            });

            // Corbeille → confirmation puis suppression
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

        // Clic sur le nom de l'auteur → ouvre son profil
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
        CircleImageView authorProfileImage;
        ImageView btnDelete, btnEdit;
        ImageView tipImage; // photo jointe au tip

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
        }
    }
}
