package com.example.medifiles;

public class Message {
    public static String SENT_BY_ME = "me";
    public static String SENT_BY_BOT="bot";
    String message;
    String sentBy;

    // 기본 생성자
    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
    }
}
