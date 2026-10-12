package com.example.movieapp.repository;

import com.example.movieapp.entities.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BannerRepo extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderBySortOrderAscIdAsc();

    @Query("SELECT COALESCE(MAX(b.sortOrder), -1) FROM Banner b")
    int findMaxSortOrder();

    @Modifying
    @Transactional
    @Query("DELETE FROM Banner b WHERE b.series.id = :seriesId")
    void deleteBySeriesId(@Param("seriesId") Long seriesId);
}
