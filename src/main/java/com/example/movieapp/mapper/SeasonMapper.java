package com.example.movieapp.mapper;

import com.example.movieapp.dto.SeasonDto;
import com.example.movieapp.entities.Season;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeasonMapper {

    @Mapping(source = "series.id", target = "seriesId")
    SeasonDto toDto(Season season);

    List<SeasonDto> toDtoList(List<Season> seasons);

}
