package com.example.movieapp.repository;

import com.example.movieapp.entities.MovieAccess;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieAccessRepository extends JpaRepository<MovieAccess, Long> {
    boolean existsByUserIdAndMovieIdAndPaidTrue(Long userId, Long movieId);

    @Query("SELECT COUNT(m) > 0 FROM MovieAccess m WHERE m.movie.id = :seriesId " +
            "AND m.paid = true AND (m.accessEndDate IS NULL OR m.accessEndDate >= CURRENT_DATE)")
    boolean existsActivePaidAccessByMovieId(@Param("seriesId") Long seriesId);

    boolean existsByUserIdAndMovieId(Long userId, Long seriesId);

    void deleteByMovie_Id(Long movieId);

    Collection<MovieAccess> findByUserIdAndPaidTrue(Long userId);

    List<MovieAccess> findByPaidTrueAndCreatedAtBefore(LocalDateTime oneMonthAgo);

    Optional<MovieAccess> findByUserIdAndMovieIdAndPaidTrue(Long userId, Long movieId);

    List<MovieAccess> findByUserId(Long userId);

    void deleteByUserIdAndPaidIsTrue(Long userId);

    Optional<MovieAccess> findByUserAndMovie(User user, Series series);

    List<MovieAccess> findByUserAndPaidIsTrue(User user);

    Optional<MovieAccess> findByUser_IdAndMovie_IdAndPaidIsTrue(Long userId, Long serialId);

    Optional<MovieAccess> findByUser_IdAndMovie_IdAndPaidIsFalse(Long userId, Long serialId);

    List<MovieAccess> findByPaidTrueAndAccessEndDateIsNotNullAndAccessEndDateBefore(java.time.LocalDate date);

}
