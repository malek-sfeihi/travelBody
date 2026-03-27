package com.example.travelbuddy;

/*
 * Modèle d'un message dans le chat.
 * Stocké dans Firebase sous "chats/{chatRoomId}/messages/{messageId}".
 * On a besoin de senderId et receiverId pour savoir
 * qui a envoyé quoi et afficher les bulles du bon côté.
 */
public class Message {
    public String message;       // le texte du message
    public String senderId;      // uid de celui qui envoie
    public String receiverId;    // uid de celui qui reçoit
    public long timestamp;       // quand le message a été envoyé

    // Constructeur vide requis par Firebase
    public Message() {}

    public Message(String message, String senderId, String receiverId, long timestamp) {
        this.message = message;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
    }
}
