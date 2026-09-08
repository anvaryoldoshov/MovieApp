package com.example.movieapp.exception;

public class SeasonNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.SEASON_NOT_FOUND; }
}
