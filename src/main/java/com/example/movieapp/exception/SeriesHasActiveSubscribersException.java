package com.example.movieapp.exception;

public class SeriesHasActiveSubscribersException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.SERIES_HAS_ACTIVE_SUBSCRIBERS; }
}
