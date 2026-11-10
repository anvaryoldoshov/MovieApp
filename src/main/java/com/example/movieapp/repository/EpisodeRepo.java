package com.example.movieapp.repository;

import com.example.movieapp.entities.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;

public interface EpisodeRepo extends JpaRepository<Episode,Long> {

    List<Episode> findBySeriesId(Long seriesId);

    boolean existsBySeriesId(Long seriesId);

    List<Episode> findByDurationMinutesIsNull();

    List<Episode> findByDurationMinutesIsNullOrFileSizeBytesIsNull();

    boolean existsBySeasonId(Long seasonId);

    List<Episode> findBySeasonIsNull();

    @Query("SELECT e FROM Episode e JOIN FETCH e.series WHERE e.publishAt IS NOT NULL AND e.publishAt <= :now " +
            "AND (e.releaseNotified IS NULL OR e.releaseNotified = false)")
    List<Episode> findReleasedNotNotified(@Param("now") Instant now);
}
