package com.example.travelbuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/*
 * Adapter pour le chat : affiche les messages dans le RecyclerView.
 * On utilise deux layouts différents selon que le message est envoyé ou reçu :
 * - item_message_sent.xml pour nos messages (bulle à droite)
 * - item_message_received.xml pour les messages de l'autre (bulle à gauche)
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // Constantes pour distinguer les deux types de cellules
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    // Détermine quel layout utiliser : si c'est mon message → SENT, sinon → RECEIVED
    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).senderId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    // On inflate le bon layout selon le type (envoyé ou reçu)
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    // On remplit la cellule avec le texte du message
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.messageText.setText(message.message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder simple : juste un TextView pour le contenu du message
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
}
