package com.example.movieapp.repository;

import com.example.movieapp.entities.NotificationSound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSoundRepository extends JpaRepository<NotificationSound, Long> {
    Optional<NotificationSound> findByValue(String value);

    List<NotificationSound> findAllByOrderByLastUsedAtDesc();
}
