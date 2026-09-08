package com.example.movieapp.exception;

public class SeasonHasEpisodesException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.SEASON_HAS_EPISODES; }
}
