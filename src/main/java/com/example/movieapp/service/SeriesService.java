package com.example.movieapp.service;

import com.example.movieapp.dto.GetDetailsResponse;
import com.example.movieapp.dto.SeriesDto;
import com.example.movieapp.dto.SeriesStatDto;
import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Series;
import com.example.movieapp.exception.SeriesHasActiveSubscribersException;
import com.example.movieapp.mapper.EpisodeMapper;
import com.example.movieapp.mapper.SeriesMapper;
import com.example.movieapp.dto.EpisodePartDto;
import com.example.movieapp.repository.BannerRepo;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.GenreRepo;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.SeasonRepo;
import com.example.movieapp.repository.SeriesRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private static final String REMOVED_STATUS = "REMOVED";

    private final SeriesRepo seriesRepo;
    private final SeriesMapper seriesMapper;
    private final EpisodeRepo episodeRepo;
    private final EpisodeMapper episodeMapper;
    private final BannerRepo bannerRepo;
    private final MovieAccessRepository movieAccessRepository;
    private final PaymentRepository paymentRepository;
    private final GenreRepo genreRepo;
    private final SeasonRepo seasonRepo;
    private final BunnyStreamService bunnyStreamService;

    @Transactional
    public Series createOrFetch(SeriesDto dto) {
        if (dto.getId() != null) {
            return seriesRepo.findById(dto.getId()).orElseThrow(() -> new RuntimeException("Series topilmadi: " + dto.getId()));
        }
        Series entity = seriesMapper.toEntity(dto);
        return seriesRepo.save(entity);
    }


    public ResponseEntity<List<SeriesDto>> findAll(boolean includeHidden) {
        List<SeriesDto> series = seriesRepo.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(s -> includeHidden || !REMOVED_STATUS.equals(s.getStatus()))
                .map(s -> {
                    SeriesDto dto = seriesMapper.toDto(s);
                    dto.setHasEpisode(episodeRepo.existsBySeriesId(s.getId()));
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(series);
    }

    public GetDetailsResponse getDetails(Long seriesId, boolean hasAccess) {
        Series series = seriesRepo.findById(seriesId).orElseThrow(() -> new RuntimeException("Series not found"));

        List<Episode> episodes = episodeRepo.findBySeriesId(seriesId);
        List<EpisodePartDto> parts = episodeMapper.toPartDtoList(episodes);

        // Bonus/bepul epizodlar (serialda "birinchi N ta epizod bepul" siyosati bo'yicha) obunasiz ham ochiq
        Integer freeCount = series.getFreeEpisodesCount();
        for (int i = 0; i < parts.size(); i++) {
            EpisodePartDto part = parts.get(i);
            boolean isFree = freeCount != null && part.getEpisodeNumber() <= freeCount;
            part.setFree(isFree);
            part.setHasAccess(hasAccess || isFree);
        }

        return GetDetailsResponse.builder().id(series.getId()).title(series.getTitle()).parts(parts).hasAccess(hasAccess).build();
    }

    public ResponseEntity<Map<String, Object>> saveSeries(SeriesDto seriesDto) {
        Series series = seriesMapper.toEntity(seriesDto);

        if (seriesDto.getImagePath() != null) {
            series.setImagePath(seriesDto.getImagePath());
        }

        if (seriesDto.getGenreIds() != null) {
            series.setGenres(genreRepo.findAllById(seriesDto.getGenreIds()));
        }

        series.setSortOrder(seriesRepo.findMaxSortOrder() + 1);
        series.setFreeEpisodesCount(seriesDto.getFreeEpisodesCount());
        series.setBunnyCollectionId(bunnyStreamService.extractCollectionId(seriesDto.getBunnyCollectionId()));

        Series saved = seriesRepo.save(series);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Series saved successfully", "id", saved.getId()));
    }

    public ResponseEntity<SeriesDto> updateSeries(Long seriesId, SeriesDto seriesDto) {
        return seriesRepo.findById(seriesId).map(series -> {
            series.setTitle(seriesDto.getTitle());
            series.setStatus(seriesDto.getStatus());
            series.setImagePath(seriesDto.getImagePath());
            series.setMonthlyPrice(seriesDto.getMonthlyPrice());
            series.setQuarterlyPrice(seriesDto.getQuarterlyPrice());
            series.setFreeEpisodesCount(seriesDto.getFreeEpisodesCount());
            series.setBunnyCollectionId(bunnyStreamService.extractCollectionId(seriesDto.getBunnyCollectionId()));
            if (seriesDto.getGenreIds() != null) {
                series.setGenres(genreRepo.findAllById(seriesDto.getGenreIds()));
            }
            Series updated = seriesRepo.save(series);

            SeriesDto dto = seriesMapper.toDto(updated);
            dto.setHasEpisode(episodeRepo.existsBySeriesId(updated.getId()));

            return ResponseEntity.ok(dto);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Adminda seriallar ro'yxatini qo'lda tartiblash (drag & drop). orderedIds ro'yxatidagi
     * ketma-ketlik yangi tartibni belgilaydi.
     */
    @Transactional
    public ResponseEntity<?> reorderSeries(List<Long> orderedIds) {
        List<Series> seriesList = seriesRepo.findAllById(orderedIds);
        Map<Long, Series> byId = seriesList.stream()
                .collect(java.util.stream.Collectors.toMap(Series::getId, s -> s));

        for (int i = 0; i < orderedIds.size(); i++) {
            Series series = byId.get(orderedIds.get(i));
            if (series != null) {
                series.setSortOrder(i);
            }
        }

        seriesRepo.saveAll(seriesList);
        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> deleteSeries(Long seriesId) {
        if (movieAccessRepository.existsActivePaidAccessByMovieId(seriesId)) {
            throw new SeriesHasActiveSubscribersException();
        }
        movieAccessRepository.deleteByMovie_Id(seriesId);
        bannerRepo.deleteBySeriesId(seriesId);
        paymentRepository.detachSeries(seriesId);
        // Epizodlar avval o'chirilishi kerak, chunki ular fasllarga bog'langan (FK)
        episodeRepo.deleteAll(episodeRepo.findBySeriesId(seriesId));
        seasonRepo.deleteBySeries_Id(seriesId);
        seriesRepo.deleteById(seriesId);
        return ResponseEntity.ok().build();
    }

    /**
     * Har bir serial/film uchun hozirgi kunda faol (to'lovli, muddati tugamagan) obunachilar soni.
     */
    public List<SeriesStatDto> getSeriesStatistics() {
        Map<Long, Long> countsBySeriesId = movieAccessRepository.countActiveSubscribersGroupedBySeries().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return seriesRepo.findAll().stream()
                .map(s -> new SeriesStatDto(
                        s.getId(),
                        s.getTitle(),
                        s.getImagePath(),
                        countsBySeriesId.getOrDefault(s.getId(), 0L)
                ))
                .sorted((a, b) -> Long.compare(b.getSubscriberCount(), a.getSubscriberCount()))
                .toList();
    }
}
