package com.example.movieapp.exception;

public class UserNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.USER_NOT_FOUND; }
}
