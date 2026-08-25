package com.example.movieapp.exception;

public class UserNotificationNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() { return ErrorCode.NOTIFICATION_NOT_FOUND; }
}
