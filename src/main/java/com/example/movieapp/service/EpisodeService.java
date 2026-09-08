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
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepo episodeRepo;
    private final EpisodeMapper episodeMapper;
    private final SeriesRepo seriesRepo;
    private final SeasonRepo seasonRepo;
    private final BunnyStreamService bunnyStreamService;
    private final FileStorageService fileStorageService;

    public EpisodeDto getEpisodeById(Long seriesId, Long episodeId) {

        Episode episode = episodeRepo.findById(episodeId)
                .orElseThrow(() -> new RuntimeException("Episode not found"));

        if (!episode.getSeries().getId().equals(seriesId)) {
            throw new EpisodeNotBelongToSeriesException();
        }

        EpisodeDto dto = episodeMapper.toEpisodeDto(episode);
        dto.setFree(isEpisodeFree(episode.getSeries(), episode.getEpisodeNumber()));
        return dto;
    }

    public EpisodeDto addEpisode(Long seriesId, EpisodeDto dto) {
        Series series = seriesRepo.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Series not found"));

        Season season = resolveSeason(series, dto.getSeasonId(), dto.getEpisodeNumber());

        Episode episode = Episode.builder()
                .title(dto.getTitle())
                .episodeNumber(dto.getEpisodeNumber())
                .thumbnail(dto.getThumbnail())
                .fileName(dto.getFileName())
                .videoUrl(dto.getVideoUrl())
                .series(series)
                .season(season)
                .build();

        Optional<BunnyStreamService.BunnyVideoInfo> bunnyInfo = applyDurationFromBunny(episode, dto.getVideoUrl());

        // Admin thumbnail yuklamagan bo'lsa, Bunny avtomatik yaratgan thumbnaildan foydalanamiz
        if ((episode.getThumbnail() == null || episode.getThumbnail().isBlank())) {
            bunnyInfo.map(BunnyStreamService.BunnyVideoInfo::thumbnailUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .map(url -> fileStorageService.saveImageFromUrl("episodes", url))
                    .ifPresent(episode::setThumbnail);
        }

        Episode saved = episodeRepo.save(episode);

        EpisodeDto resultDto = episodeMapper.toEpisodeDto(saved);
        resultDto.setFree(isEpisodeFree(series, saved.getEpisodeNumber()));
        return resultDto;
    }

    /**
     * Fasl admin tomonidan aniq ko'rsatilmagan bo'lsa, epizod raqami va serialdagi fasllarning
     * rejalashtirilgan sig'imiga (episodeCount) qarab avtomatik tanlaydi: masalan 1-fasl=30,
     * 2-fasl=10 deb belgilangan bo'lsa, 31-epizod avtomatik 2-faslga tushadi. Hech qanday fasl
     * mavjud bo'lmasa, standart "1-fasl" yaratiladi.
     */
    private Season resolveSeason(Series series, Long explicitSeasonId, Integer episodeNumber) {
        if (explicitSeasonId != null) {
            Season season = seasonRepo.findById(explicitSeasonId).orElseThrow(SeasonNotFoundException::new);
            if (!season.getSeries().getId().equals(series.getId())) {
                throw new SeasonNotFoundException();
            }
            return season;
        }

        List<Season> seasons = seasonRepo.findBySeries_IdOrderBySeasonNumberAsc(series.getId());
        if (seasons.isEmpty()) {
            Season newSeason = new Season();
            newSeason.setSeries(series);
            newSeason.setSeasonNumber(1);
            newSeason.setTitle("1-fasl");
            return seasonRepo.save(newSeason);
        }

        if (episodeNumber == null) {
            return seasons.get(0);
        }

        int cumulative = 0;
        for (Season season : seasons) {
            Integer capacity = season.getEpisodeCount();
            if (capacity == null || capacity <= 0) {
                // Sig'imi belgilanmagan fasl - shu nuqtadan keyingi hamma epizod shu faslga tushadi
                return season;
            }
            cumulative += capacity;
            if (episodeNumber <= cumulative) {
                return season;
            }
        }

        // Barcha fasllarning belgilangan sig'imidan oshib ketdi - oxirgi faslga qo'shiladi
        return seasons.get(seasons.size() - 1);
    }

    /**
     * Serialda "birinchi N ta epizod bepul" siyosati belgilangan bo'lsa, epizod raqamiga
     * qarab bepul-emasligini hisoblaydi. Bu holat saqlanmaydi - har doim jonli hisoblanadi,
     * shuning uchun freeEpisodesCount o'zgarganda barcha epizodlar uchun avtomatik yangilanadi.
     */
    private boolean isEpisodeFree(Series series, Integer episodeNumber) {
        Integer freeCount = series.getFreeEpisodesCount();
        return episodeNumber != null && freeCount != null && episodeNumber <= freeCount;
    }

    private Optional<BunnyStreamService.BunnyVideoInfo> applyDurationFromBunny(Episode episode, String videoUrl) {
        Optional<BunnyStreamService.BunnyVideoInfo> infoOpt = bunnyStreamService.fetchVideoInfo(videoUrl);
        infoOpt.ifPresentOrElse(info -> {
            int totalSeconds = info.durationSeconds();
            episode.setDurationHours(totalSeconds / 3600);
            episode.setDurationMinutes((totalSeconds % 3600) / 60);
            episode.setDurationSeconds(totalSeconds % 60);
            episode.setFileSizeBytes(info.sizeBytes());
        }, () -> log.warn("Episode uchun Bunny'dan video ma'lumoti olinmadi, videoUrl={}", videoUrl));
        return infoOpt;
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
            if (applyDurationFromBunny(episode, episode.getVideoUrl()).isPresent()) {
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
                        Optional<BunnyStreamService.BunnyVideoInfo> bunnyInfo = applyDurationFromBunny(episode, dto.getVideoUrl());

                        // Admin yangi thumbnail yuklamagan bo'lsa, yangi video uchun Bunny thumbnaili olinadi
                        if (dto.getThumbnail() == null || dto.getThumbnail().isBlank()) {
                            bunnyInfo.map(BunnyStreamService.BunnyVideoInfo::thumbnailUrl)
                                    .filter(url -> url != null && !url.isBlank())
                                    .map(url -> fileStorageService.saveImageFromUrl("episodes", url))
                                    .ifPresent(episode::setThumbnail);
                        }
                    }

                    // Fasl: admin aniq ko'rsatsa o'shanga, aks holda (yoki epizod raqami o'zgargan bo'lsa)
                    // sig'imga qarab avtomatik qayta hisoblanadi
                    if (dto.getSeasonId() != null || dto.getEpisodeNumber() != null) {
                        Season season = resolveSeason(episode.getSeries(), dto.getSeasonId(), episode.getEpisodeNumber());
                        episode.setSeason(season);
                    }

                    Episode updated = episodeRepo.save(episode);

                    EpisodeDto resultDto = episodeMapper.toEpisodeDto(updated);
                    resultDto.setFree(isEpisodeFree(updated.getSeries(), updated.getEpisodeNumber()));

                    return ResponseEntity.ok(resultDto);
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
                .map(episode -> {
                    EpisodeDto dto = episodeMapper.toEpisodeDto(episode);
                    dto.setFree(isEpisodeFree(episode.getSeries(), episode.getEpisodeNumber()));
                    return dto;
                })
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
