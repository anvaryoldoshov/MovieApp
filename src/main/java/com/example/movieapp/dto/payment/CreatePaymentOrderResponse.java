package com.example.movieapp.dto.payment;

import com.example.movieapp.enums.PaymentProvider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentOrderResponse {
    private Long orderId;
    private Long amount;      // tiyin
    private Long amountInSom; // so'm
    private PaymentProvider provider;
    private String payUrl;    // Payme yoki Click checkout URL
}
