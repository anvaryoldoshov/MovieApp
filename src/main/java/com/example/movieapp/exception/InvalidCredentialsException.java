package com.example.movieapp.exception;

public class InvalidCredentialsException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.INVALID_CREDENTIALS; }
}
