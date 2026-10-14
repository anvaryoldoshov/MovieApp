package com.example.movieapp.exception;

public class PromoCodeException extends AppException {

    private final ErrorCode errorCode;

    public PromoCodeException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCode errorCode() {
        return errorCode;
    }
}
