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
@Table(name = "watch_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "episode_id"}))
public class WatchProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "episode_id")
    private Episode episode;

    private int positionSeconds; // foydalanuvchi shu epizodni necha soniyagacha ko'rgani

    private Instant updatedAt;
}
