package com.example.movieapp.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.example.movieapp.entities.Payment;
import com.example.movieapp.enums.PaymentProvider;
import com.example.movieapp.enums.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPixyHash(String pixyHash);

    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.series = null WHERE p.series.id = :seriesId")
    void detachSeries(@Param("seriesId") Long seriesId);

    void deleteByUserId(Long userId);

    boolean existsByUser_IdAndPromoCode_IdAndStatus(Long userId, Long promoCodeId, PaymentStatus status);

    long countByPromoCode_IdAndStatus(Long promoCodeId, PaymentStatus status);

    long countByPromoCode_Id(Long promoCodeId);

    List<Payment> findByPromoCode_IdOrderByCreatedAtDesc(Long promoCodeId);

    /** Har bir promokod bo'yicha: to'langan soni, to'lagan odamlar, jami chegirma, jami tushum (tiyin), qo'llab ko'rgan odamlar */
    @Query("SELECT p.promoCode.id, " +
            "SUM(CASE WHEN p.status = com.example.movieapp.enums.PaymentStatus.PAID THEN 1 ELSE 0 END), " +
            "COUNT(DISTINCT CASE WHEN p.status = com.example.movieapp.enums.PaymentStatus.PAID THEN p.user.id END), " +
            "COALESCE(SUM(CASE WHEN p.status = com.example.movieapp.enums.PaymentStatus.PAID THEN p.discountAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.status = com.example.movieapp.enums.PaymentStatus.PAID THEN p.amount ELSE 0 END), 0), " +
            "COUNT(DISTINCT p.user.id) " +
            "FROM Payment p WHERE p.promoCode IS NOT NULL GROUP BY p.promoCode.id")
    List<Object[]> promoCodeStats();

    List<Payment> findByUser_IdAndSeries_IdAndProviderAndStatus(Long userId, Long seriesId,
                                                                 PaymentProvider provider, PaymentStatus status);

    @Query(value = "SELECT to_char(created_at, 'YYYY-MM') AS month, COUNT(*), COUNT(DISTINCT user_id), " +
            "COALESCE(SUM(CASE WHEN provider = 'MANUAL' THEN amount / 100 ELSE ROUND(amount / 104.0) END), 0), " +
            "COALESCE(SUM(amount / 100), 0), " +
            "COUNT(*) FILTER (WHERE provider = 'MANUAL'), " +
            "COALESCE(SUM(amount / 100) FILTER (WHERE provider = 'MANUAL'), 0) " +
            "FROM payments WHERE series_id = :seriesId AND status = 'PAID' " +
            "GROUP BY month ORDER BY month DESC", nativeQuery = true)
    List<Object[]> monthlyStatsBySeries(@Param("seriesId") Long seriesId);
}
