package com.example.movieapp.entities;

import com.example.movieapp.enums.BotPurchaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bot_purchases", indexes = {
        @Index(name = "idx_bot_purchases_telegram", columnList = "telegramId"),
        @Index(name = "idx_bot_purchases_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String purchaseId;

    @Column(nullable = false)
    private Long telegramId;

    @Column(nullable = false)
    private Long botSeriesId;

    private String title;

    private Integer planDays;

    private Instant purchasedAt;

    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotPurchaseStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "series_id")
    private Series series;

    private LocalDate appliedEndDate;

    private Instant appliedAt;

    @Column(nullable = false)
    private Instant receivedAt;
}
