package com.example.movieapp.dto.promo;

import com.example.movieapp.enums.DiscountType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PromoCodeRequest {
    private String code;
    private DiscountType discountType;
    private Long discountValue;
    private Integer maxUses;      // null = cheksiz
    private LocalDate validUntil; // null = muddatsiz
    private Boolean active;
}
