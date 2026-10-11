package com.example.movieapp.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.example.movieapp.entities.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPixyHash(String pixyHash);

    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.series = null WHERE p.series.id = :seriesId")
    void detachSeries(@Param("seriesId") Long seriesId);

    void deleteByUserId(Long userId);

    @Query(value = "SELECT to_char(created_at, 'YYYY-MM') AS month, COUNT(*), COUNT(DISTINCT user_id), " +
            "COALESCE(SUM(ROUND(amount / 104.0)), 0), COALESCE(SUM(amount / 100), 0) " +
            "FROM payments WHERE series_id = :seriesId AND status = 'PAID' " +
            "GROUP BY month ORDER BY month DESC", nativeQuery = true)
    List<Object[]> monthlyStatsBySeries(@Param("seriesId") Long seriesId);
}
