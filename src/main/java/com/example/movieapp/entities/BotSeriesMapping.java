package com.example.movieapp.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bot_series_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotSeriesMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long botSeriesId;

    private String botTitle;

    @ManyToOne
    @JoinColumn(name = "series_id")
    private Series series;

    private Instant createdAt;

    private Instant updatedAt;
}
