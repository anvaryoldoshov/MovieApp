package com.example.movieapp.service;

import com.example.movieapp.dto.ContinueWatchingDto;
import com.example.movieapp.entities.Episode;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.entities.WatchProgress;
import com.example.movieapp.exception.EpisodeNotFoundException;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.EpisodeRepo;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.repository.WatchProgressRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WatchProgressService {

    private final WatchProgressRepo watchProgressRepo;
    private final EpisodeRepo episodeRepo;
    private final UserRepo userRepo;

    @Transactional
    public void saveProgress(Long userId, Long episodeId, int positionSeconds) {
        Episode episode = episodeRepo.findById(episodeId).orElseThrow(EpisodeNotFoundException::new);
        User user = userRepo.findById(userId).orElseThrow(UserNotFoundException::new);

        WatchProgress progress = watchProgressRepo.findByUser_IdAndEpisode_Id(userId, episodeId)
                .orElseGet(() -> WatchProgress.builder().user(user).episode(episode).build());
        progress.setPositionSeconds(positionSeconds);
        progress.setUpdatedAt(Instant.now());
        watchProgressRepo.save(progress);
    }

    /**
     * Bir nechta epizodning ko'rilgan soniyalarini bitta so'rovda oladi
     * (epizodlar ro'yxatiga watchedSeconds biriktirish uchun).
     */
    public Map<Long, Integer> getProgressMap(Long userId, List<Long> episodeIds) {
        if (episodeIds.isEmpty()) {
            return Map.of();
        }
        return watchProgressRepo.findByUser_IdAndEpisode_IdIn(userId, episodeIds).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.getEpisode().getId(), WatchProgress::getPositionSeconds));
    }

    public List<ContinueWatchingDto> getContinueWatching(Long userId, int limit) {
        return watchProgressRepo.findByUser_IdOrderByUpdatedAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::toContinueWatchingDto)
                .toList();
    }

    private ContinueWatchingDto toContinueWatchingDto(WatchProgress progress) {
        Episode episode = progress.getEpisode();
        Series series = episode.getSeries();
        return ContinueWatchingDto.builder()
                .seriesId(series.getId())
                .seriesTitle(series.getTitle())
                .seriesImagePath(series.getImagePath())
                .episodeId(episode.getId())
                .episodeNumber(episode.getEpisodeNumber())
                .episodeTitle(episode.getTitle())
                .episodeThumbnail(episode.getThumbnail())
                .positionSeconds(progress.getPositionSeconds())
                .durationSeconds(totalDurationSeconds(episode))
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    private static int totalDurationSeconds(Episode episode) {
        int hours = episode.getDurationHours() == null ? 0 : episode.getDurationHours();
        int minutes = episode.getDurationMinutes() == null ? 0 : episode.getDurationMinutes();
        int seconds = episode.getDurationSeconds() == null ? 0 : episode.getDurationSeconds();
        return hours * 3600 + minutes * 60 + seconds;
    }
}
