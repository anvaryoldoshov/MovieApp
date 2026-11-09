package com.example.movieapp.repository;

import com.example.movieapp.entities.TelegramLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface TelegramLoginRepository extends JpaRepository<TelegramLogin, Long> {
    Optional<TelegramLogin> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying
    @Transactional
    @Query("DELETE FROM TelegramLogin t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(Instant before);
}
