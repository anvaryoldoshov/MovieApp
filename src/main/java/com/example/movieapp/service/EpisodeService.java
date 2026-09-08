package com.example.movieapp.service;

import com.example.movieapp.dto.EpisodeDto;
import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Season;
import com.example.movieapp.entities.Series;
import com.example.movieapp.mapper.EpisodeMapper;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.SeasonRepo;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.exception.EpisodeNotBelongToSeriesException;
import com.example.movieapp.exception.SeasonNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepo episodeRepo;
    private final EpisodeMapper episodeMapper;
    private final SeriesRepo seriesRepo;
    private final SeasonRepo seasonRepo;
    private final BunnyStreamService bunnyStreamService;

    public EpisodeDto getEpisodeById(Long seriesId, Long episodeId) {

        Episode episode = episodeRepo.findById(episodeId)
                .orElseThrow(() -> new RuntimeException("Episode not found"));

        if (!episode.getSeries().getId().equals(seriesId)) {
            throw new EpisodeNotBelongToSeriesException();
        }

        return episodeMapper.toEpisodeDto(episode);
    }

    public Episode addEpisode(Long seriesId, EpisodeDto dto) {
        Series series = seriesRepo.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Series not found"));

        Season season = resolveSeason(series, dto.getSeasonId());

        Episode episode = Episode.builder()
                .title(dto.getTitle())
                .episodeNumber(dto.getEpisodeNumber())
                .thumbnail(dto.getThumbnail())
                .fileName(dto.getFileName())
                .videoUrl(dto.getVideoUrl())
                .series(series)
                .season(season)
                .free(dto.isFree())
                .build();

        applyDurationFromBunny(episode, dto.getVideoUrl());

        return episodeRepo.save(episode);
    }

    /**
     * seasonId ko'rsatilmagan bo'lsa (masalan eski admin so'rovlari), serialning
     * standart "1-fasl"ini topib yoki yaratib qaytaradi.
     */
    private Season resolveSeason(Series series, Long seasonId) {
        if (seasonId != null) {
            Season season = seasonRepo.findById(seasonId).orElseThrow(SeasonNotFoundException::new);
            if (!season.getSeries().getId().equals(series.getId())) {
                throw new SeasonNotFoundException();
            }
            return season;
        }

        return seasonRepo.findBySeries_IdAndSeasonNumber(series.getId(), 1).orElseGet(() -> {
            Season newSeason = new Season();
            newSeason.setSeries(series);
            newSeason.setSeasonNumber(1);
            newSeason.setTitle("1-fasl");
            return seasonRepo.save(newSeason);
        });
    }

    private boolean applyDurationFromBunny(Episode episode, String videoUrl) {
        return bunnyStreamService.fetchVideoInfo(videoUrl).map(info -> {
            int totalSeconds = info.durationSeconds();
            episode.setDurationHours(totalSeconds / 3600);
            episode.setDurationMinutes((totalSeconds % 3600) / 60);
            episode.setDurationSeconds(totalSeconds % 60);
            episode.setFileSizeBytes(info.sizeBytes());
            return true;
        }).orElseGet(() -> {
            log.warn("Episode uchun Bunny'dan video ma'lumoti olinmadi, videoUrl={}", videoUrl);
            return false;
        });
    }

    /**
     * Duration yoki fayl hajmi hali yozilmagan eski episode'larni Bunny Stream API
     * orqali bir martalik to'ldiradi.
     */
    public Map<String, Object> backfillMissingDurations() {
        List<Episode> episodes = episodeRepo.findByDurationMinutesIsNullOrFileSizeBytesIsNull();

        int updated = 0;
        int failed = 0;
        for (Episode episode : episodes) {
            if (applyDurationFromBunny(episode, episode.getVideoUrl())) {
                episodeRepo.save(episode);
                updated++;
            } else {
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", episodes.size());
        result.put("updated", updated);
        result.put("failed", failed);
        return result;
    }


    // EpisodeService.java

    public ResponseEntity<EpisodeDto> updateEpisode(Long episodeId, EpisodeDto dto) {
        return episodeRepo.findById(episodeId)
                .map(episode -> {
                    // Sarlavha (Title) mavjud bo'lsa yangilanadi
                    if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                        episode.setTitle(dto.getTitle());
                    }

                    // Epizod raqami mavjud bo'lsa yangilanadi
                    if (dto.getEpisodeNumber() != null) {
                        episode.setEpisodeNumber(dto.getEpisodeNumber());
                    }

                    // Rasmni yangilash yo'li (Thumbnail) - bu endi faylning serverdagi yangi manzili
                    if (dto.getThumbnail() != null && !dto.getThumbnail().isBlank()) {
                        // Agar eski rasm bor bo'lsa, uni o'chirish logikasi shu yerda qo'shilishi mumkin (ixtiyoriy)
                        episode.setThumbnail(dto.getThumbnail());
                    }

                    // Fayl nomi (agar ishlatilsa)
                    if (dto.getFileName() != null && !dto.getFileName().isBlank()) {
                        episode.setFileName(dto.getFileName());
                    }

                    // Video URL mavjud bo'lsa yangilanadi, davomiylik Bunny'dan qayta olinadi
                    if (dto.getVideoUrl() != null && !dto.getVideoUrl().isBlank()) {
                        episode.setVideoUrl(dto.getVideoUrl());
                        applyDurationFromBunny(episode, dto.getVideoUrl());
                    }

                    // Faslni ko'chirish (boshqa faslga)
                    if (dto.getSeasonId() != null) {
                        Season season = seasonRepo.findById(dto.getSeasonId())
                                .orElseThrow(SeasonNotFoundException::new);
                        if (!season.getSeries().getId().equals(episode.getSeries().getId())) {
                            throw new SeasonNotFoundException();
                        }
                        episode.setSeason(season);
                    }

                    // Bonus/bepul epizod belgisi
                    episode.setFree(dto.isFree());

                    Episode updated = episodeRepo.save(episode);

                    return ResponseEntity.ok(episodeMapper.toEpisodeDto(updated));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    public ResponseEntity<Map<String, String>> deleteEpisode(Long episodeId) {
        return episodeRepo.findById(episodeId)
                .map(ep -> {
                    episodeRepo.delete(ep);
                    return ResponseEntity.ok(Map.of("message", "Episode deleted"));
                }).orElse(ResponseEntity.notFound().build());
    }

    public List<EpisodeDto> getEpisodesBySeries(Long seriesId) {
        List<Episode> episodes = episodeRepo.findBySeriesId(seriesId);

        return episodes.stream()
                .map(episodeMapper::toEpisodeDto)
                .toList();
    }

    /**
     * Obunasi bor-yo'qligidan qat'i nazar, muddati cheklangan token bilan imzolangan
     * video havolasi qaytariladi; hasAccess belgisi frontendga obuna holatini bildirish uchun saqlanadi.
     */
    public void finalizeVideoUrlForAccess(EpisodeDto dto, boolean hasAccess) {
        dto.setHasAccess(hasAccess);
        dto.setVideoUrl(bunnyStreamService.signPlaybackUrl(dto.getVideoUrl()));
    }

}
