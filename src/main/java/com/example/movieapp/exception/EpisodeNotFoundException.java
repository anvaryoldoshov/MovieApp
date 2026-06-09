package com.example.movieapp.exception;

public class EpisodeNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.EPISODE_NOT_FOUND; }
}
