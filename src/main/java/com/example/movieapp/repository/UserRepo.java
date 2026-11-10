package com.example.movieapp.repository;

import com.example.movieapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByTelegramChatId(Long telegramChatId);

    boolean existsByUserId(Long userId);

    // Admin push'ni har bir foydalanuvchining inbox'iga yozish uchun (butun User obyektlarini yuklamasdan).
    @Query("SELECT u.id FROM User u")
    List<Long> findAllIds();

    @Query("SELECT u.id FROM User u WHERE u.subscription = true " +
            "AND (u.subscriptionEndDate IS NULL OR u.subscriptionEndDate >= CURRENT_DATE)")
    List<Long> findActiveSubscriberIds();

    @Query("SELECT u.id FROM User u WHERE u.subscription = true OR u.subscriptionEndDate IS NOT NULL")
    List<Long> findEverSubscribedIds();

}
