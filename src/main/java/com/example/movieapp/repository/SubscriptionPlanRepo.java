package com.example.movieapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.movieapp.entities.SubscriptionPlan;

public interface SubscriptionPlanRepo extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByActiveTrue();
}
