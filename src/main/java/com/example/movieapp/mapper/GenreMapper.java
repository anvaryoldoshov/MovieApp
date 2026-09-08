package com.example.movieapp.mapper;

import com.example.movieapp.dto.GenreDto;
import com.example.movieapp.entities.Genre;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreDto toDto(Genre genre);

    List<GenreDto> toDtoList(List<Genre> genres);

    Genre toEntity(GenreDto dto);

}
