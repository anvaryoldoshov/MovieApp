package com.example.movieapp.repository;

import com.example.movieapp.entities.SeriesReminder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeriesReminderRepo extends JpaRepository<SeriesReminder, Long> {

    boolean existsBySeries_IdAndUser_Id(Long seriesId, Long userId);

    Optional<SeriesReminder> findBySeries_IdAndUser_Id(Long seriesId, Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<SeriesReminder> findBySeries_Id(Long seriesId);

    @Modifying
    @Query("delete from SeriesReminder r where r.series.id = :seriesId")
    void deleteBySeriesId(@Param("seriesId") Long seriesId);

    @Modifying
    @Query("delete from SeriesReminder r where r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
