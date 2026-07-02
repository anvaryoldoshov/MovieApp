package com.example.movieapp.service;

import com.example.movieapp.dto.BannerDto;
import com.example.movieapp.dto.HomeResponse;
import com.example.movieapp.dto.SeriesDto;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.mapper.SeriesMapper;
import com.example.movieapp.mapper.UserMapper;
import com.example.movieapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserRepo userRepo;
    private final SeriesRepo seriesRepo;
    private final BannerRepo bannerRepo;
    private final EpisodeRepo episodeRepo;
    private final MovieAccessRepository movieAccessRepository;
    private final UserMapper userMapper;
    private final SeriesMapper seriesMapper;


    public HomeResponse getHomeData(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();
        Set<Long> accessIds = movieAccessRepository.findByUserId(userId).stream()
                .filter(ma -> !ma.isPaid() ||
                        (ma.getAccessEndDate() == null || !today.isAfter(ma.getAccessEndDate())))
                .map(ma -> ma.getMovie().getId())
                .collect(Collectors.toSet());

        List<SeriesDto> seriesList = seriesRepo.findAll().stream()
                .map(series -> {
                    SeriesDto dto = seriesMapper.toDto(series);
                    dto.setHasAccess(accessIds.contains(series.getId()));
                    dto.setHasEpisode(episodeRepo.existsBySeriesId(series.getId()));
                    return dto;
                })
                .toList();

        List<BannerDto> bannerDtos = bannerRepo.findAll().stream()
                .map(banner -> {
                    Series series = banner.getSeries();
                    SeriesDto movie = null;
                    if (series != null) {
                        movie = seriesMapper.toDto(series);
                        movie.setHasAccess(accessIds.contains(series.getId()));
                        movie.setHasEpisode(episodeRepo.existsBySeriesId(series.getId()));
                    }
                    return BannerDto.builder()
                            .image(banner.getImage())
                            .movie(movie)
                            .build();
                }).toList();

        return HomeResponse.builder()
                .user(userMapper.toUserDto(user))
                .series(seriesList)
                .banners(bannerDtos)
                .build();
    }
}

