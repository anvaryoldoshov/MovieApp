package com.example.movieapp.dto.payment;

import lombok.Data;

@Data
public class CreatePaymentOrderRequest {
    private Long seriesId;
    private Integer durationMonths; // 1 yoki 3
}
