package com.example.travelbuddy;

import com.google.firebase.database.IgnoreExtraProperties;

/*
 * Classe modèle qui représente une astuce de voyage.
 * Chaque tip est stocké dans Firebase sous "tips/{id}".
 * On garde les champs publics pour que Firebase puisse
 * les lire/écrire directement (sérialisation automatique).
 */
@IgnoreExtraProperties
public class Tip {
    public String id;
    public String title;
    public String description;
    public String location;      // nom du lieu (ex: "Sidi Bou Said")
    public String country;       // pays sélectionné (ex: "Tunisie", "France")
    public String userId;        // uid de l'auteur (celui qui a posté)
    public String imageUrl;      // image en Base64 ou null
    public long timestamp;       // date de publication en millisecondes

    // Constructeur vide obligatoire pour Firebase
    public Tip() {}

    // Constructeur complet
    public Tip(String id, String title, String description, String location, String country, String userId, String imageUrl, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.country = country;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }
}
