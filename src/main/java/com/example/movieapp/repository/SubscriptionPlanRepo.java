package com.example.movieapp.repository;

import com.example.movieapp.entities.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPlanRepo extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByActiveTrue();
}
