package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeriesMonthlyStatDto {

    private Long seriesId;
    private String title;
    private String imagePath;
    private long activeSubscribers;
    private long totalPurchases;
    private long totalAmount;
    private long totalPaidAmount;
    private List<Month> months;

    /** amount — tarif narxi bo'yicha (komissiyasiz), paidAmount — foydalanuvchi to'lagan summa (4% komissiya bilan), so'mda. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Month {
        private String month;
        private long purchases;
        private long buyers;
        private long amount;
        private long paidAmount;
    }
}
