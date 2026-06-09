package com.example.movieapp.exception;

public class MovieAccessNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.MOVIE_ACCESS_NOT_FOUND; }
}
