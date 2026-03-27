package com.example.travelbuddy;

/*
 * Modèle simplifié pour une conversation.
 * On stocke juste l'id et le nom de l'autre utilisateur
 * pour pouvoir afficher la liste des conversations.
 */
public class Conversation {
    public String otherUserId;    // uid de l'interlocuteur
    public String otherUserName;  // nom affiché dans la liste

    // Constructeur vide pour Firebase
    public Conversation() {}

    public Conversation(String otherUserId, String otherUserName) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
    }
}
