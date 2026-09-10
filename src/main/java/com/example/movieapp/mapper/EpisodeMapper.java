package com.example.movieapp.mapper;

import com.example.movieapp.dto.EpisodeDto;
import com.example.movieapp.dto.EpisodePartDto;
import com.example.movieapp.entities.Episode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EpisodeMapper {

    @Mapping(source = "series.id", target = "seriesId")
    @Mapping(target = "hasAccess", ignore = true)
    @Mapping(target = "free", ignore = true)
    @Mapping(target = "watchedSeconds", ignore = true)
    @Mapping(target = "fileSizeMb", expression = "java(episode.getFileSizeBytes() == null ? null : episode.getFileSizeBytes() / (1024.0 * 1024.0))")
    @Mapping(source = "season.id", target = "seasonId")
    @Mapping(source = "season.seasonNumber", target = "seasonNumber")
    EpisodeDto toEpisodeDto(Episode episode);

    @Mapping(source = "id", target = "episodeId")
    @Mapping(target = "hasAccess", ignore = true)
    @Mapping(target = "free", ignore = true)
    @Mapping(target = "watchedSeconds", ignore = true)
    @Mapping(source = "season.id", target = "seasonId")
    @Mapping(source = "season.seasonNumber", target = "seasonNumber")
    @Mapping(source = "season.title", target = "seasonTitle")
    @Mapping(target = "durationSeconds", expression = "java(" +
            "(episode.getDurationHours() == null ? 0 : episode.getDurationHours() * 3600) + " +
            "(episode.getDurationMinutes() == null ? 0 : episode.getDurationMinutes() * 60) + " +
            "(episode.getDurationSeconds() == null ? 0 : episode.getDurationSeconds())" +
            ")")
    EpisodePartDto toPartDto(Episode episode);

    List<EpisodePartDto> toPartDtoList(List<Episode> episodes);
}

