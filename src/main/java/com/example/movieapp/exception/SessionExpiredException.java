package com.example.movieapp.exception;

public class SessionExpiredException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.SESSION_EXPIRED; }
}
