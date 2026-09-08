package com.example.movieapp.exception;

public class GenreAlreadyExistsException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.GENRE_ALREADY_EXISTS; }
}
