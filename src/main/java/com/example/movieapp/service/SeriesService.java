package com.example.movieapp.service;

import com.example.movieapp.dto.GetDetailsResponse;
import com.example.movieapp.dto.SeriesDto;
import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Series;
import com.example.movieapp.mapper.EpisodeMapper;
import com.example.movieapp.mapper.SeriesMapper;
import com.example.movieapp.repository.BannerRepo;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.SeriesRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepo seriesRepo;
    private final SeriesMapper seriesMapper;
    private final EpisodeRepo episodeRepo;
    private final EpisodeMapper episodeMapper;
    private final BannerRepo bannerRepo;
    private final MovieAccessRepository movieAccessRepository;

    @Transactional
    public Series createOrFetch(SeriesDto dto) {
        if (dto.getId() != null) {
            return seriesRepo.findById(dto.getId()).orElseThrow(() -> new RuntimeException("Series topilmadi: " + dto.getId()));
        }
        Series entity = seriesMapper.toEntity(dto);
        return seriesRepo.save(entity);
    }


    public ResponseEntity<List<SeriesDto>> findAll() {
        List<SeriesDto> series = seriesRepo.findAll().stream()
                .map(s -> {
                    SeriesDto dto = seriesMapper.toDto(s);
                    dto.setHasEpisode(episodeRepo.existsBySeriesId(s.getId()));
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(series);
    }

    public GetDetailsResponse getDetails(Long seriesId) {
        Series series = seriesRepo.findById(seriesId).orElseThrow(() -> new RuntimeException("Series not found"));

        List<Episode> episodes = episodeRepo.findBySeriesId(seriesId);

        return GetDetailsResponse.builder().id(series.getId()).title(series.getTitle()).parts(episodeMapper.toPartDtoList(episodes)).build();
    }

    public ResponseEntity<Map<String, Object>> saveSeries(SeriesDto seriesDto) {
        Series series = seriesMapper.toEntity(seriesDto);

        if (seriesDto.getImagePath() != null) {
            series.setImagePath(seriesDto.getImagePath());
        }

        Series saved = seriesRepo.save(series);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Series saved successfully", "id", saved.getId()));
    }

    public ResponseEntity<Map<String, Object>> updateSeries(Long seriesId, SeriesDto seriesDto) {
        return seriesRepo.findById(seriesId).map(series -> {
            series.setTitle(seriesDto.getTitle());
            series.setStatus(seriesDto.getStatus());
            series.setImagePath(seriesDto.getImagePath());
            series.setMonthlyPrice(seriesDto.getMonthlyPrice());
            series.setQuarterlyPrice(seriesDto.getQuarterlyPrice());
            Series updated = seriesRepo.save(series);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Series updated successfully");
            response.put("id", updated.getId());

            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    public ResponseEntity<?> deleteSeries(Long seriesId) {
        movieAccessRepository.deleteByMovie_Id(seriesId);
        bannerRepo.deleteBySeriesId(seriesId);
        seriesRepo.deleteById(seriesId);
        return ResponseEntity.ok().build();
    }
}
