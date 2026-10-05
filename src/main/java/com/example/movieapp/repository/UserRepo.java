package com.example.movieapp.repository;

import com.example.movieapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    boolean existsByUserId(Long userId);

    // Admin push'ni har bir foydalanuvchining inbox'iga yozish uchun (butun User obyektlarini yuklamasdan).
    @Query("SELECT u.id FROM User u")
    List<Long> findAllIds();

}
