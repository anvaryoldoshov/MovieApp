package com.example.movieapp.dto;

import lombok.Data;

@Data
public class SubscriptionPlanDto {
    private Long id;
    private String name;
    private String description;
    private Long monthlyPrice;
    private Long quarterlyPrice;
    private boolean active;
}
