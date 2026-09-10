package com.example.movieapp.dto;

import lombok.Data;

@Data
public class EpisodePartDto {
    private Long episodeId;
    private int episodeNumber;
    private String title;
    private String thumbnail;
    private boolean free;
    private boolean hasAccess;
    private Long seasonId;
    private Integer seasonNumber;
    private String seasonTitle;
    private int watchedSeconds; // foydalanuvchi shu epizodni necha soniyagacha ko'rgani
    private int durationSeconds;
}
