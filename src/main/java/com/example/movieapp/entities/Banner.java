package com.example.movieapp.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "banners")
@Data
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String image;

    private Integer sortOrder; // ilovada va adminda ko'rinish tartibi (kichik = birinchi)

    @ManyToOne
    @JoinColumn(name = "series_id")
    @JsonBackReference
    private Series series;
}

