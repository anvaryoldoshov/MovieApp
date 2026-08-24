package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeriesStatDto {

    private Long seriesId;
    private String title;
    private String imagePath;
    private long subscriberCount;
}
