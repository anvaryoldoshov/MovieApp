package com.example.movieapp.repository;

import com.example.movieapp.entities.WatchProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchProgressRepo extends JpaRepository<WatchProgress, Long> {

    Optional<WatchProgress> findByUser_IdAndEpisode_Id(Long userId, Long episodeId);

    List<WatchProgress> findByUser_IdAndEpisode_IdIn(Long userId, List<Long> episodeIds);

    List<WatchProgress> findByUser_IdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("delete from WatchProgress w where w.episode.id in (select e.id from Episode e where e.series.id = :seriesId)")
    void deleteBySeriesId(@Param("seriesId") Long seriesId);

    @Modifying
    @Query("delete from WatchProgress w where w.episode.id = :episodeId")
    void deleteByEpisodeId(@Param("episodeId") Long episodeId);
}
