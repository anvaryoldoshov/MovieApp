package com.example.movieapp.service;

import com.example.movieapp.dto.SubscriptionPlanDto;
import com.example.movieapp.entities.SubscriptionPlan;
import com.example.movieapp.exception.SubscriptionPlanNotFoundException;
import com.example.movieapp.repository.SubscriptionPlanRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepo subscriptionPlanRepo;

    /** Foydalanuvchiga ko'rsatiladigan faol tariflar */
    public List<SubscriptionPlan> findAllActive() {
        return subscriptionPlanRepo.findByActiveTrue();
    }

    /** Admin uchun: faol va nofaol barcha tariflar */
    public List<SubscriptionPlan> findAll() {
        return subscriptionPlanRepo.findAll();
    }

    public SubscriptionPlan findById(Long id) {
        return subscriptionPlanRepo.findById(id)
                .orElseThrow(SubscriptionPlanNotFoundException::new);
    }

    public SubscriptionPlan create(SubscriptionPlanDto dto) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setMonthlyPrice(dto.getMonthlyPrice());
        plan.setQuarterlyPrice(dto.getQuarterlyPrice());
        plan.setActive(dto.isActive());
        return subscriptionPlanRepo.save(plan);
    }

    public SubscriptionPlan update(Long id, SubscriptionPlanDto dto) {
        SubscriptionPlan plan = findById(id);
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setMonthlyPrice(dto.getMonthlyPrice());
        plan.setQuarterlyPrice(dto.getQuarterlyPrice());
        plan.setActive(dto.isActive());
        return subscriptionPlanRepo.save(plan);
    }

    public void delete(Long id) {
        SubscriptionPlan plan = findById(id);
        subscriptionPlanRepo.delete(plan);
    }
}
