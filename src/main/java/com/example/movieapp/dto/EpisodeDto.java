package com.example.movieapp.dto;

import lombok.Data;

@Data
public class EpisodeDto {
    private Long id;

    private Long seriesId;

    private String title;

    private Integer episodeNumber;

    private String thumbnail;

    private String videoUrl;

    private String fileName;

    private Integer durationHours;

    private Integer durationMinutes;

    private Integer durationSeconds;

    private boolean hasAccess;

}
