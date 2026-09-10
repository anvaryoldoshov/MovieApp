package com.example.movieapp.controller;

import com.example.movieapp.dto.ContinueWatchingDto;
import com.example.movieapp.dto.EpisodeDto;
import com.example.movieapp.dto.WatchProgressRequest;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.EpisodeService;
import com.example.movieapp.service.MovieAccessService;
import com.example.movieapp.service.WatchProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class EpisodeController {
    private final EpisodeService episodeService;
    private final UserRepo userRepo;
    private final MovieAccessService movieAccessService;
    private final WatchProgressService watchProgressService;

    @GetMapping("/{seriesId}/episodes")
    public ResponseEntity<List<EpisodeDto>> getEpisodesBySeries(
            @PathVariable Long seriesId,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        boolean hasAccess = movieAccessService.canUserWatchMovie(user.getId(), seriesId);
        List<EpisodeDto> episodes = episodeService.getEpisodesBySeries(seriesId);
        episodes.forEach(ep -> episodeService.finalizeVideoUrlForAccess(ep, hasAccess));

        Map<Long, Integer> progressMap = watchProgressService.getProgressMap(user.getId(),
                episodes.stream().map(EpisodeDto::getId).toList());
        episodes.forEach(ep -> ep.setWatchedSeconds(progressMap.getOrDefault(ep.getId(), 0)));

        return ResponseEntity.ok(episodes);
    }

    @PostMapping("/{seriesId}/episode/{episodeId}/progress")
    public ResponseEntity<?> saveProgress(@PathVariable Long seriesId, @PathVariable Long episodeId,
                                          @Valid @RequestBody WatchProgressRequest request,
                                          Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        watchProgressService.saveProgress(user.getId(), episodeId, request.getPositionSeconds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/continue-watching")
    public ResponseEntity<List<ContinueWatchingDto>> continueWatching(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        return ResponseEntity.ok(watchProgressService.getContinueWatching(user.getId(), 20));
    }
}
