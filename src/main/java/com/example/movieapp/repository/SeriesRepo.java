package com.example.movieapp.repository;

import com.example.movieapp.entities.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeriesRepo extends JpaRepository<Series, Long> {

    List<Series> findByGenres_Id(Long genreId);

}
