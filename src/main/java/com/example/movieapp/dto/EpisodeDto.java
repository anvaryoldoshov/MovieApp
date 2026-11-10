package com.example.movieapp.dto;

import lombok.Data;

import java.time.Instant;

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

    private Double fileSizeMb;

    private boolean hasAccess;

    private boolean free; // true bo'lsa, obunasiz ham hamma tomosha qila oladi (bonus epizod)

    private Long seasonId;

    private Integer seasonNumber;

    private int watchedSeconds; // foydalanuvchi shu epizodni necha soniyagacha ko'rgani

    private Instant publishAt;

    private boolean scheduled;

    private boolean reminded;

    private Boolean notifyBuyers;

}
