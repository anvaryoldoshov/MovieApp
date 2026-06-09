package com.example.movieapp.exception;

public class UserAlreadyExistsException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.USER_ALREADY_EXISTS; }
}
