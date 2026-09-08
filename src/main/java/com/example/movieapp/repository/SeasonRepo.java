package com.example.movieapp.repository;

import com.example.movieapp.entities.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepo extends JpaRepository<Season, Long> {

    List<Season> findBySeries_IdOrderBySeasonNumberAsc(Long seriesId);

    Optional<Season> findBySeries_IdAndSeasonNumber(Long seriesId, Integer seasonNumber);

    boolean existsBySeries_IdAndSeasonNumber(Long seriesId, Integer seasonNumber);

    void deleteBySeries_Id(Long seriesId);

}
