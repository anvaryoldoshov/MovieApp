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
}
