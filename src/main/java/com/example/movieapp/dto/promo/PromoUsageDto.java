package com.example.movieapp.dto.promo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PromoUsageDto {
    private Long paymentId;
    private Long userId;
    private String username;
    private String email;
    private String target;        // serial nomi yoki obuna tarifi
    private Integer days;
    private Long amountInSom;     // to'langan summa (komissiya bilan)
    private Long discountInSom;
    private String status;
    private LocalDateTime createdAt;
}
