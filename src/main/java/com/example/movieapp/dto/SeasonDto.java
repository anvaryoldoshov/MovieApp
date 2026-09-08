package com.example.movieapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeasonDto {

    private Long id;

    private Long seriesId;

    @NotNull
    private Integer seasonNumber;

    private String title;

    private Integer episodeCount; // shu faslga rejalashtirilgan epizodlar soni (avtomatik taqsimlash uchun)

}
