package com.example.movieapp.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "seasons")
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    private Integer seasonNumber;

    private String title; // ixtiyoriy, masalan "Maxsus qism" (bo'sh bo'lsa "N-fasl" ko'rsatiladi)

}
