package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Barcha to'langan xaridlar: jami va joriy oy bo'yicha soni va summasi (so'mda, tarif narxi bo'yicha). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesSummaryDto {

    private long totalPurchases;
    private long totalAmount;
    private long monthPurchases;
    private long monthAmount;
}
