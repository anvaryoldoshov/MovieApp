package com.example.movieapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BotPurchaseRequest {

    @JsonProperty("purchase_id")
    private String purchaseId;

    @JsonProperty("telegram_id")
    private Long telegramId;

    @JsonProperty("series_id")
    private Long seriesId;

    private String title;

    @JsonProperty("plan_days")
    private Integer planDays;

    @JsonProperty("purchased_at")
    private String purchasedAt;

    @JsonProperty("expires_at")
    private String expiresAt;

    private String source;
}
