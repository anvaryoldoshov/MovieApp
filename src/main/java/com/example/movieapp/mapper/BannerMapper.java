package com.example.movieapp.mapper;

import com.example.movieapp.dto.AuthResponse;
import com.example.movieapp.dto.BannerDto;
import com.example.movieapp.dto.UserDto;
import com.example.movieapp.entities.Banner;
import com.example.movieapp.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SeriesMapper.class)
public interface BannerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "movie", target = "series", qualifiedByName = "seriesDtoToEntityById")
    Banner toBanner(BannerDto bannerDto);
}

