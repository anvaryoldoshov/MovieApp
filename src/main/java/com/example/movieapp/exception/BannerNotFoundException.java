package com.example.movieapp.exception;

public class BannerNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.BANNER_NOT_FOUND; }
}
