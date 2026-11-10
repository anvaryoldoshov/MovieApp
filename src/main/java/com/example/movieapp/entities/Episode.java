package com.example.movieapp.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "episodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String videoUrl;

    private Integer episodeNumber;

    private String title;

    private String thumbnail;

    private String fileName;

    private Integer durationHours;

    private Integer durationMinutes;

    private Integer durationSeconds;

    private Long fileSizeBytes;

    @ManyToOne
    @JoinColumn(name = "series_id")
    @JsonBackReference
    private Series series;

    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    // null = darhol ko'rinadi; kelajakdagi vaqt = shu vaqtgacha ilovada "Tez kunda" bo'lib turadi
    private Instant publishAt;

    // Chiqqan paytda eslatma so'raganlarga (va notifyBuyers bo'lsa xaridorlarga) push yuborilganmi
    private Boolean releaseNotified;

    private Boolean notifyBuyers;

    public boolean isScheduled() {
        return publishAt != null && publishAt.isAfter(Instant.now());
    }
}
