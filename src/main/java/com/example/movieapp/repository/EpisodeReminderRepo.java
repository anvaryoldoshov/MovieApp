package com.example.movieapp.repository;

import com.example.movieapp.entities.EpisodeReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EpisodeReminderRepo extends JpaRepository<EpisodeReminder, Long> {

    Optional<EpisodeReminder> findByEpisode_IdAndUser_Id(Long episodeId, Long userId);

    @Query("SELECT r.episode.id FROM EpisodeReminder r WHERE r.user.id = :userId AND r.episode.id IN :episodeIds")
    List<Long> findRemindedEpisodeIds(@Param("userId") Long userId, @Param("episodeIds") Collection<Long> episodeIds);

    @Query("SELECT r.user.id FROM EpisodeReminder r WHERE r.episode.id = :episodeId")
    List<Long> findUserIdsByEpisodeId(@Param("episodeId") Long episodeId);

    @Modifying
    @Query("DELETE FROM EpisodeReminder r WHERE r.episode.id = :episodeId")
    void deleteByEpisodeId(@Param("episodeId") Long episodeId);

    @Modifying
    @Query("DELETE FROM EpisodeReminder r WHERE r.episode.id IN (SELECT e.id FROM Episode e WHERE e.series.id = :seriesId)")
    void deleteBySeriesId(@Param("seriesId") Long seriesId);

    @Modifying
    @Query("DELETE FROM EpisodeReminder r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
