package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Mobil ilova profilida ko'rsatiladigan obuna holati:
 * umumiy obuna (subscriptionBased seriallar uchun) va alohida sotib olingan seriallar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MySubscriptionDto {
    private boolean active;          // umumiy obuna hozir amal qiladimi
    private LocalDate endDate;       // umumiy obuna tugash sanasi (null — obuna yo'q)
    private long daysLeft;           // tugashiga necha kun qoldi (0 — tugagan/yo'q)
    private List<SeriesAccess> series; // alohida sotib olingan, hali amal qiladigan seriallar

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesAccess {
        private Long seriesId;
        private String title;
        private String imagePath;
        private LocalDate endDate;
        private long daysLeft;
    }
}
