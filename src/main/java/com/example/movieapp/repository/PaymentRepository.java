package com.example.movieapp.repository;

import com.example.movieapp.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPixyHash(String pixyHash);

    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.series = null WHERE p.series.id = :seriesId")
    void detachSeries(@Param("seriesId") Long seriesId);
}
