package com.example.movieapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TelegramConfirmRequest {

    @JsonProperty("login_code")
    private String loginCode;

    private TelegramUser user;

    @Data
    public static class TelegramUser {
        @JsonProperty("chat_id")
        private Long chatId;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String username;

        @JsonProperty("photo_url")
        private String photoUrl;

        private List<Subscription> subscriptions;
    }

    @Data
    public static class Subscription {
        @JsonProperty("series_id")
        private Long seriesId;

        private String title;

        @JsonProperty("expires_at")
        private String expiresAt;

        @JsonProperty("days_left")
        private Integer daysLeft;

        @JsonProperty("plan_days")
        private Integer planDays;

        @JsonProperty("purchased_at")
        private String purchasedAt;

        private String source;
    }
}
