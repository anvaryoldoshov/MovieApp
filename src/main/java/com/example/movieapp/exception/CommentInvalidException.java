package com.example.movieapp.exception;

public class CommentInvalidException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.COMMENT_INVALID; }
}
