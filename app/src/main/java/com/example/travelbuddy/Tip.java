package com.example.travelbuddy;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Tip {
    public String id;
    public String title;
    public String description;
    public String location;
    public String userId;
    public String imageUrl;
    public long timestamp;

    public Tip() {} // Constructeur par défaut requis pour Firebase

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
