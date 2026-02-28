package com.example.travelbuddy;

public class Message {
    public String message;
    public String senderId;
    public String receiverId;
    public long timestamp;

    public Message() {}

    public Message(String message, String senderId, String receiverId, long timestamp) {
        this.message = message;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
    }
}
