package com.example.movieapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SeriesDto {

    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String status;

    private String imagePath;

    private boolean hasAccess;

    private boolean hasEpisode;

    private Long monthlyPrice;   // null bo'lsa 1 oylik tarif yo'q
    private Long quarterlyPrice; // null bo'lsa 3 oylik tarif yo'q

    private List<Long> genreIds;   // janrlarni biriktirish uchun (create/update)
    private List<GenreDto> genres; // javobda ko'rsatish uchun

    private Integer freeEpisodesCount; // birinchi N ta epizod obunasiz ham ochiq (null/0 = yo'q)

}