package com.example.movieapp.exception;

public class SeasonAlreadyExistsException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.SEASON_ALREADY_EXISTS; }
}
