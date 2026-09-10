package com.example.movieapp.repository;

import com.example.movieapp.entities.SeriesLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeriesLikeRepo extends JpaRepository<SeriesLike, Long> {

    Optional<SeriesLike> findBySeries_IdAndUser_Id(Long seriesId, Long userId);

    boolean existsBySeries_IdAndUser_Id(Long seriesId, Long userId);

    long countBySeries_Id(Long seriesId);

    void deleteBySeries_IdAndUser_Id(Long seriesId, Long userId);
}
