package com.example.movieapp.controller;

import com.example.movieapp.dto.SeasonDto;
import com.example.movieapp.service.SeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSeasonController {

    private final SeasonService seasonService;

    @GetMapping("/admin/series/{seriesId}/seasons")
    public ResponseEntity<List<SeasonDto>> getSeasons(@PathVariable Long seriesId) {
        return ResponseEntity.ok(seasonService.getSeasonsBySeries(seriesId));
    }

    @PostMapping("/admin/series/{seriesId}/seasons")
    public ResponseEntity<SeasonDto> createSeason(@PathVariable Long seriesId, @Valid @RequestBody SeasonDto dto) {
        return ResponseEntity.ok(seasonService.createSeason(seriesId, dto));
    }

    @PutMapping("/admin/seasons/{id}")
    public ResponseEntity<SeasonDto> updateSeason(@PathVariable Long id, @Valid @RequestBody SeasonDto dto) {
        return ResponseEntity.ok(seasonService.updateSeason(id, dto));
    }

    @DeleteMapping("/admin/seasons/{id}")
    public ResponseEntity<?> deleteSeason(@PathVariable Long id) {
        seasonService.deleteSeason(id);
        return ResponseEntity.ok().build();
    }

}
