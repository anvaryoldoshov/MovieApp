package com.example.movieapp.repository;

import com.example.movieapp.entities.BotPurchase;
import com.example.movieapp.enums.BotPurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface BotPurchaseRepository extends JpaRepository<BotPurchase, Long> {
    boolean existsByPurchaseId(String purchaseId);

    List<BotPurchase> findByTelegramIdAndStatusIn(Long telegramId, Collection<BotPurchaseStatus> statuses);

    List<BotPurchase> findByBotSeriesIdAndStatus(Long botSeriesId, BotPurchaseStatus status);

    List<BotPurchase> findTop200ByOrderByReceivedAtDesc();

    List<BotPurchase> findTop200ByStatusOrderByReceivedAtDesc(BotPurchaseStatus status);

    @Query("SELECT p.botSeriesId, COUNT(p) FROM BotPurchase p WHERE p.status = com.example.movieapp.enums.BotPurchaseStatus.PENDING_MAPPING GROUP BY p.botSeriesId")
    List<Object[]> countPendingMappingByBotSeries();
}
