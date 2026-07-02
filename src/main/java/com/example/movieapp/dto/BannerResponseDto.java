package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponseDto {
    private Long id;
    private String image;
    private Long seriesId;
    private String seriesTitle;
    private boolean hasEpisode;
}