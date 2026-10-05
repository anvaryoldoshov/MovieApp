package com.example.movieapp.repository;

import com.example.movieapp.entities.SeriesLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeriesLikeRepo extends JpaRepository<SeriesLike, Long> {

    // Foydalanuvchining sevimlilari, oxirgi like birinchi.
    List<SeriesLike> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<SeriesLike> findBySeries_IdAndUser_Id(Long seriesId, Long userId);

    boolean existsBySeries_IdAndUser_Id(Long seriesId, Long userId);

    long countBySeries_Id(Long seriesId);

    void deleteBySeries_IdAndUser_Id(Long seriesId, Long userId);
}
