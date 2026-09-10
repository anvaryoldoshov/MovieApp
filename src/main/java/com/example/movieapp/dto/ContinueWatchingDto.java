package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContinueWatchingDto {
    private Long seriesId;
    private String seriesTitle;
    private String seriesImagePath;

    private Long episodeId;
    private Integer episodeNumber;
    private String episodeTitle;
    private String episodeThumbnail;

    private int positionSeconds;
    private int durationSeconds;

    private Instant updatedAt;
}
