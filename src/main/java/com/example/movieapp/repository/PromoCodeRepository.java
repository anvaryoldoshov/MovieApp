package com.example.movieapp.repository;

import com.example.movieapp.entities.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /** Serial o'chirilganda uni promokodlardan ham olib tashlash */
    @Modifying
    @Query(value = "DELETE FROM promo_code_series WHERE series_id = :seriesId", nativeQuery = true)
    void detachSeries(@Param("seriesId") Long seriesId);
}
