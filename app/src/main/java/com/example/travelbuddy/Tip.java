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
    public String userId;        // uid de l'auteur (celui qui a posté)
    public String imageUrl;      // url image (pas encore utilisé dans l'app)
    public long timestamp;       // date de publication en millisecondes

    // Constructeur vide obligatoire pour que Firebase puisse
    // désérialiser les données en objet Tip
    public Tip() {}

    // Constructeur complet, utilisé quand on crée un nouveau tip
    public Tip(String id, String title, String description, String location, String userId, String imageUrl, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }
}
