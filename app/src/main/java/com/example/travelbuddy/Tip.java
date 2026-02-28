package com.example.travelbuddy;

public class Tip {
    public String id;
    public String title;
    public String description;
    public String location;
    public String userId;

    public Tip() {} // Constructeur par défaut requis pour Firebase


    public Tip(String id, String title, String description, String location, String userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.userId = userId;
    }
}
