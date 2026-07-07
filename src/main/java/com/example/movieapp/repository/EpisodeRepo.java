package com.example.movieapp.repository;

import com.example.movieapp.entities.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeRepo extends JpaRepository<Episode,Long> {

    List<Episode> findBySeriesId(Long seriesId);

    boolean existsBySeriesId(Long seriesId);

    List<Episode> findByDurationMinutesIsNull();

    List<Episode> findByDurationMinutesIsNullOrFileSizeBytesIsNull();
}
