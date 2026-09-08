package com.example.movieapp.service;

import com.example.movieapp.dto.GenreDto;
import com.example.movieapp.entities.Genre;
import com.example.movieapp.entities.Series;
import com.example.movieapp.exception.GenreAlreadyExistsException;
import com.example.movieapp.exception.GenreNotFoundException;
import com.example.movieapp.mapper.GenreMapper;
import com.example.movieapp.repository.GenreRepo;
import com.example.movieapp.repository.SeriesRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepo genreRepo;
    private final GenreMapper genreMapper;
    private final SeriesRepo seriesRepo;

    public List<GenreDto> findAll() {
        return genreMapper.toDtoList(genreRepo.findAll());
    }

    public GenreDto create(GenreDto dto) {
        if (genreRepo.existsByNameIgnoreCase(dto.getName())) {
            throw new GenreAlreadyExistsException();
        }
        Genre genre = new Genre();
        genre.setName(dto.getName());
        return genreMapper.toDto(genreRepo.save(genre));
    }

    public GenreDto update(Long id, GenreDto dto) {
        Genre genre = genreRepo.findById(id).orElseThrow(GenreNotFoundException::new);

        if (!genre.getName().equalsIgnoreCase(dto.getName()) && genreRepo.existsByNameIgnoreCase(dto.getName())) {
            throw new GenreAlreadyExistsException();
        }

        genre.setName(dto.getName());
        return genreMapper.toDto(genreRepo.save(genre));
    }

    @Transactional
    public void delete(Long id) {
        Genre genre = genreRepo.findById(id).orElseThrow(GenreNotFoundException::new);

        List<Series> seriesWithGenre = seriesRepo.findByGenres_Id(id);
        for (Series series : seriesWithGenre) {
            series.getGenres().removeIf(g -> g.getId().equals(id));
        }
        seriesRepo.saveAll(seriesWithGenre);

        genreRepo.delete(genre);
    }

}
