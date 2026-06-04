package com.example.movieapp.dto.payment;

import lombok.Data;

@Data
public class CreatePaymentOrderRequest {
    private Long userId;
    private Integer subscriptionDays; // 30, 90, 365
    private Long seriesId;            // individual series uchun
}
