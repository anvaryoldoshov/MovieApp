package com.example.movieapp.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // masalan: "Premium Obuna"

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long monthlyPrice;   // 1 oylik narx (so'mda), null = mavjud emas
    private Long quarterlyPrice; // 3 oylik narx (so'mda), null = mavjud emas

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
