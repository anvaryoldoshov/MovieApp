package com.example.movieapp.dto.payment;

import lombok.Data;

@Data
public class CreatePaymentOrderRequest {
    private Long seriesId;           // Individual serial uchun; obuna to'lovida null
    private Long subscriptionPlanId; // Obuna to'lovi uchun; individual serialda null
    private Integer durationMonths;  // 1 yoki 3
}
