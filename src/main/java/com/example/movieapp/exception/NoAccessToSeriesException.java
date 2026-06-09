package com.example.movieapp.exception;

public class NoAccessToSeriesException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.NO_ACCESS_TO_SERIES; }
}
