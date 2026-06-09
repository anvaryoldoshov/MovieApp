package com.example.movieapp.exception;

public class SeriesNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.SERIES_NOT_FOUND; }
}
