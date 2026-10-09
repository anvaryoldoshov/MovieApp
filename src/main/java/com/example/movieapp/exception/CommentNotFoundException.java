package com.example.movieapp.exception;

public class CommentNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.COMMENT_NOT_FOUND; }
}
