package com.example.movieapp.dto.payment;

import com.example.movieapp.enums.PaymentProvider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentOrderResponse {
    private Long orderId;
    private Integer durationMonths; // 1 yoki 3
    private Integer accessDays;     // 30 yoki 90
    private Long originalAmountInSom; // chegirmagacha narx (komissiyasiz)
    private Long discountInSom;       // promokod chegirmasi, bo'lmasa 0
    private String promoCode;
    private Long baseAmountInSom;   // serial narxi (komissiyasiz, chegirmadan keyin)
    private Long commissionInSom;   // 4% komissiya
    private Long amountInSom;       // jami (base + komissiya)
    private Long amount;            // jami tiyin
    private PaymentProvider provider;
    private String payUrl;
}
