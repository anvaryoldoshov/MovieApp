package com.example.movieapp.controller;

import com.example.movieapp.dto.EpisodeDto;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

//@GetMapping("/{sid}/episode/{eid}")
//public ResponseEntity<?> getEpisode(
//        @PathVariable Long sid,
//        @PathVariable Long eid
//) {
//    return ResponseEntity.ok(episodeService.getEpisodeById(sid, eid));
//}

@GetMapping("/{seriesId}/episodes")
public ResponseEntity<List<EpisodeDto>> getEpisodesBySeries(@PathVariable Long seriesId) {
    List<EpisodeDto> episodes = episodeService.getEpisodesBySeries(seriesId);
    return ResponseEntity.ok(episodes);
}
}
