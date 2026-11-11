package com.example.movieapp.entities;

import com.example.movieapp.enums.TelegramLoginStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "telegram_logins", indexes = @Index(name = "idx_telegram_logins_code", columnList = "code", unique = true))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TelegramLoginStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant confirmedAt;

    private Long chatId;

    private String firstName;

    private String lastName;

    private String tgUsername;

    @Column(length = 128)
    private String codeVerifier;
}
