package com.example.travelbuddy;

public class Conversation {
    public String otherUserId;
    public String otherUserName;

    public Conversation() {}

    public Conversation(String otherUserId, String otherUserName) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
    }
}
