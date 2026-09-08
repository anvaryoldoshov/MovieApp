package com.example.movieapp.exception;

public class GenreNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.GENRE_NOT_FOUND; }
}
