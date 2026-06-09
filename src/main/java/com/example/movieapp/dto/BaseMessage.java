package com.example.movieapp.dto;

public class BaseMessage {
    private int code;
    private String message;

    public BaseMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
