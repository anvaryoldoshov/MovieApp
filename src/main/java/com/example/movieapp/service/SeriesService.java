package com.example.movieapp.service;

import com.example.movieapp.dto.GetDetailsResponse;
import com.example.movieapp.dto.SeriesDto;
import com.example.movieapp.dto.SeriesLikeResponse;
import com.example.movieapp.dto.SeriesStatDto;
import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.SeriesLike;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.SeriesHasActiveSubscribersException;
import com.example.movieapp.exception.SeriesNotFoundException;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.mapper.EpisodeMapper;
import com.example.movieapp.mapper.SeriesMapper;
import com.example.movieapp.dto.EpisodePartDto;
import com.example.movieapp.repository.BannerRepo;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.GenreRepo;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.SeasonRepo;
import com.example.movieapp.repository.SeriesLikeRepo;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeriesService {

    // Admin bo'lmagan (mobil) foydalanuvchilarga faqat shu statusdagi seriallar ko'rinadi -
    // boshqa barcha holatlar (UNLISTED, DRAFT, ARCHIVED, REMOVED) admin tomonidan aniq
    // "Efirda"ga o'tkazilmaguncha yashirin hisoblanadi. HomeService ham shu ro'yxatdan foydalanadi.
    public static final Set<String> VISIBLE_STATUSES = Set.of("PUBLISHED", "COMING_SOON");

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
    private final SeriesLikeRepo seriesLikeRepo;
    private final UserRepo userRepo;
    private final WatchProgressService watchProgressService;

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
                .filter(s -> includeHidden || VISIBLE_STATUSES.contains(s.getStatus()))
                .map(s -> {
                    SeriesDto dto = seriesMapper.toDto(s);
                    dto.setHasEpisode(episodeRepo.existsBySeriesId(s.getId()));
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(series);
    }

    @Transactional
    public GetDetailsResponse getDetails(Long seriesId, boolean hasAccess, Long userId) {
        Series series = seriesRepo.findById(seriesId).orElseThrow(SeriesNotFoundException::new);

        series.setViewCount(series.getViewCount() + 1);
        seriesRepo.save(series);

        List<Episode> episodes = episodeRepo.findBySeriesId(seriesId);
        List<EpisodePartDto> parts = episodeMapper.toPartDtoList(episodes);

        // Bonus/bepul epizodlar (serialda "birinchi N ta epizod bepul" siyosati bo'yicha) obunasiz ham ochiq
        Integer freeCount = series.getFreeEpisodesCount();
        Map<Long, Integer> progressMap = userId != null
                ? watchProgressService.getProgressMap(userId, parts.stream().map(EpisodePartDto::getEpisodeId).toList())
                : Map.of();
        for (int i = 0; i < parts.size(); i++) {
            EpisodePartDto part = parts.get(i);
            boolean isFree = freeCount != null && part.getEpisodeNumber() <= freeCount;
            part.setFree(isFree);
            part.setHasAccess(hasAccess || isFree);
            part.setWatchedSeconds(progressMap.getOrDefault(part.getEpisodeId(), 0));
        }

        long likeCount = seriesLikeRepo.countBySeries_Id(seriesId);
        boolean liked = userId != null && seriesLikeRepo.existsBySeries_IdAndUser_Id(seriesId, userId);

        return GetDetailsResponse.builder()
                .id(series.getId())
                .title(series.getTitle())
                .parts(parts)
                .hasAccess(hasAccess)
                .likeCount(likeCount)
                .liked(liked)
                .viewCount(series.getViewCount())
                .build();
    }

    /**
     * Foydalanuvchi serialga like bosadi/olib tashlaydi (toggle).
     */
    @Transactional
    public SeriesLikeResponse toggleLike(Long seriesId, Long userId) {
        Series series = seriesRepo.findById(seriesId).orElseThrow(SeriesNotFoundException::new);
        User user = userRepo.findById(userId).orElseThrow(UserNotFoundException::new);

        boolean liked;
        Optional<SeriesLike> existing = seriesLikeRepo.findBySeries_IdAndUser_Id(seriesId, userId);
        if (existing.isPresent()) {
            seriesLikeRepo.delete(existing.get());
            liked = false;
        } else {
            SeriesLike like = SeriesLike.builder()
                    .series(series)
                    .user(user)
                    .createdAt(Instant.now())
                    .build();
            seriesLikeRepo.save(like);
            liked = true;
        }

        long likeCount = seriesLikeRepo.countBySeries_Id(seriesId);
        return new SeriesLikeResponse(liked, likeCount);
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
