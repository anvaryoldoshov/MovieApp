package com.example.movieapp.exception;

public class CommentTooFrequentException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.COMMENT_TOO_FREQUENT; }
}
