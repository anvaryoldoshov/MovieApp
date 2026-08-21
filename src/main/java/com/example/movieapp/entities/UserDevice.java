package com.example.movieapp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;

    @OneToOne
    private User user;

    private String token;

    private String fcmToken;

    private Instant createdAt;
}
