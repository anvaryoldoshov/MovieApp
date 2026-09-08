package com.example.movieapp.controller;

import com.example.movieapp.dto.GenreDto;
import com.example.movieapp.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/genres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGenreController {

    private final GenreService genreService;

    @GetMapping
    public ResponseEntity<List<GenreDto>> getAllGenres() {
        return ResponseEntity.ok(genreService.findAll());
    }

    @PostMapping
    public ResponseEntity<GenreDto> createGenre(@Valid @RequestBody GenreDto dto) {
        return ResponseEntity.ok(genreService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenreDto> updateGenre(@PathVariable Long id, @Valid @RequestBody GenreDto dto) {
        return ResponseEntity.ok(genreService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGenre(@PathVariable Long id) {
        genreService.delete(id);
        return ResponseEntity.ok().build();
    }

}
