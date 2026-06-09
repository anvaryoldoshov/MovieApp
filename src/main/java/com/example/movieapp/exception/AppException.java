package com.example.movieapp.exception;

public abstract class AppException extends RuntimeException {

    public abstract ErrorCode errorCode();

    @Override
    public String getMessage() {
        return errorCode().getMessage();
    }
}
