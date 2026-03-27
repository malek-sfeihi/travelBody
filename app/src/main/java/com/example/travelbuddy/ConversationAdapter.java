package com.example.travelbuddy;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/*
 * Adapter pour la liste des conversations.
 * Chaque item affiche l'avatar et le nom de l'interlocuteur,
 * ainsi qu'un texte d'aperçu.
 * Quand on clique sur une conversation, on ouvre le ChatActivity.
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversationList;

    public ConversationAdapter(List<Conversation> conversationList) {
        this.conversationList = conversationList;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.name.setText(conversation.otherUserName);
        holder.preview.setText("Tap to start chatting");

        // Avatar généré via UI Avatars (gratuit, sans clé API)
        // Chaque nom donne un avatar unique avec des couleurs aléatoires
        if (conversation.otherUserName != null) {
            String avatarUrl = "https://ui-avatars.com/api/?name="
                    + conversation.otherUserName.replace(" ", "+")
                    + "&background=random&color=fff&size=128";
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar_placeholder)
                    .into(holder.avatar);
        }

        // Clic sur la conversation → ouvre le chat avec cette personne
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
            intent.putExtra("userId", conversation.otherUserId);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    // ViewHolder : avatar + nom + aperçu
    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        TextView name, preview;
        CircleImageView avatar;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.conversationName);
            preview = itemView.findViewById(R.id.conversationPreview);
            avatar = itemView.findViewById(R.id.conversationAvatar);
        }
    }
}
