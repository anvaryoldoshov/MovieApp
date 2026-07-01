package com.example.movieapp.service;

import com.example.movieapp.dto.EpisodeDto;
import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Series;
import com.example.movieapp.mapper.EpisodeMapper;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.exception.EpisodeNotBelongToSeriesException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepo episodeRepo;
    private final EpisodeMapper episodeMapper;
    private final SeriesRepo seriesRepo;

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

        Episode episode = Episode.builder()
                .title(dto.getTitle())
                .episodeNumber(dto.getEpisodeNumber())
                .thumbnail(dto.getThumbnail())
                .fileName(dto.getFileName())
                .videoUrl(dto.getVideoUrl())
                .durationHours(dto.getDurationHours())
                .durationMinutes(dto.getDurationMinutes())
                .durationSeconds(dto.getDurationSeconds())
                .series(series)
                .build();

        return episodeRepo.save(episode);
    }


    // EpisodeService.java

    public ResponseEntity<Map<String, Object>> updateEpisode(Long episodeId, EpisodeDto dto) {
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

                    // Video URL mavjud bo'lsa yangilanadi
                    if (dto.getVideoUrl() != null && !dto.getVideoUrl().isBlank()) {
                        episode.setVideoUrl(dto.getVideoUrl());
                    }

                    if (dto.getDurationHours() != null) {
                        episode.setDurationHours(dto.getDurationHours());
                    }

                    if (dto.getDurationMinutes() != null) {
                        episode.setDurationMinutes(dto.getDurationMinutes());
                    }

                    if (dto.getDurationSeconds() != null) {
                        episode.setDurationSeconds(dto.getDurationSeconds());
                    }

                    episodeRepo.save(episode);

                    // Javob xabarini qaytarish
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Episode muvaffaqiyatli yangilandi");
                    response.put("id", episode.getId());
                    // Yangilangan DTO ni qaytarish ham mumkin:
                    // response.put("episode", episodeMapper.toEpisodeDto(episode));

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Episode topilmadi")));
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

}
