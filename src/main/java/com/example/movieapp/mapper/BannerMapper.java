package com.example.movieapp.mapper;

import com.example.movieapp.dto.BannerDto;
import com.example.movieapp.entities.Banner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SeriesMapper.class)
public interface BannerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "movie", target = "series", qualifiedByName = "seriesDtoToEntityById")
    Banner toBanner(BannerDto bannerDto);
}

