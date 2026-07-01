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
    EpisodeDto toEpisodeDto(Episode episode);

    @Mapping(source = "id", target = "episodeId")
    EpisodePartDto toPartDto(Episode episode);

    List<EpisodePartDto> toPartDtoList(List<Episode> episodes);
}

