package com.example.movieapp.exception;

public class SubscriptionPlanNotFoundException extends AppException {
    @Override
    public ErrorCode errorCode() {
        return ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND;
    }
}
