package com.example.movieapp.exception;

public class InvalidDeleteCodeException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.INVALID_DELETE_CODE; }
}
