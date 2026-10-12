package com.example.movieapp.repository;

import com.example.movieapp.entities.SeriesLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeriesLikeRepo extends JpaRepository<SeriesLike, Long> {

    // Foydalanuvchining sevimlilari, oxirgi like birinchi.
    List<SeriesLike> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<SeriesLike> findBySeries_IdAndUser_Id(Long seriesId, Long userId);

    boolean existsBySeries_IdAndUser_Id(Long seriesId, Long userId);

    long countBySeries_Id(Long seriesId);

    void deleteBySeries_IdAndUser_Id(Long seriesId, Long userId);

    @Modifying
    @Query("delete from SeriesLike l where l.series.id = :seriesId")
    void deleteBySeriesId(@Param("seriesId") Long seriesId);
}
