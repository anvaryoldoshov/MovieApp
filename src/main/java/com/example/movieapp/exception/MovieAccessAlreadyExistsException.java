package com.example.movieapp.exception;

public class MovieAccessAlreadyExistsException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.MOVIE_ACCESS_ALREADY_EXISTS; }
}
