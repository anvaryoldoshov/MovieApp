package com.example.movieapp.repository;

import com.example.movieapp.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPixyHash(String pixyHash);
}
