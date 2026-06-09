package com.example.movieapp.exception;

public class SessionNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.SESSION_NOT_FOUND; }
}
