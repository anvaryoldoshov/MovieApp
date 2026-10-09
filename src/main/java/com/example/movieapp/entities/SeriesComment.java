package com.example.movieapp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "series_comments", indexes = @Index(columnList = "series_id, created_at"))
public class SeriesComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id")
    private Series series;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Admin yashirgan izoh foydalanuvchilarga ko'rinmaydi, lekin bazada qoladi.
    @Builder.Default
    @Column(nullable = false)
    private boolean hidden = false;

    @Builder.Default
    @Column(nullable = false)
    private int reportCount = 0;
}
