package com.example.movieapp.repository;

import com.example.movieapp.entities.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime since);

    long countByUser_IdAndReadFalseAndCreatedAtAfter(Long userId, LocalDateTime since);

    Optional<UserNotification> findByIdAndUser_Id(Long id, Long userId);

    List<UserNotification> findByUser_IdAndReadFalseAndCreatedAtAfter(Long userId, LocalDateTime since);
}
