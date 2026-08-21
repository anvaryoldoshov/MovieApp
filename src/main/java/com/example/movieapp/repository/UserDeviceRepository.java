package com.example.movieapp.repository;

import com.example.movieapp.entities.User;
import com.example.movieapp.entities.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByUser(User user);
    void deleteByUser(User user);

    Optional<UserDevice> findByUserId(Long userId);

    Optional<UserDevice> findByUserAndDeviceId(User user, String deviceId);

    @Query("SELECT d.fcmToken FROM UserDevice d WHERE d.fcmToken IS NOT NULL")
    List<String> findAllFcmTokens();

}
