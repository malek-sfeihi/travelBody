package com.example.travelbuddy;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/*
 * Adapter pour la liste des conversations.
 * Chaque item affiche le nom de l'interlocuteur et un texte d'aperçu.
 * Quand on clique sur une conversation, on ouvre le ChatActivity
 * avec l'userId de l'autre personne.
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

    // ViewHolder : nom de l'interlocuteur + aperçu du dernier message
    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        TextView name, preview;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.conversationName);
            preview = itemView.findViewById(R.id.conversationPreview);
        }
    }
}
