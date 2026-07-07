package com.example.movieapp.controller;

import com.example.movieapp.dto.EpisodeDto;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.EpisodeService;
import com.example.movieapp.service.MovieAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class EpisodeController {
    private final EpisodeService episodeService;
    private final UserRepo userRepo;
    private final MovieAccessService movieAccessService;

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
        return ResponseEntity.ok(episodes);
    }
}
