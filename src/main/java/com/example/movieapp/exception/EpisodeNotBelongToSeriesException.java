package com.example.movieapp.exception;

public class EpisodeNotBelongToSeriesException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.EPISODE_NOT_BELONG_TO_SERIES; }
}
