package com.example.movieapp.controller;

import com.example.movieapp.dto.SubscriptionPlanDto;
import com.example.movieapp.entities.SubscriptionPlan;
import com.example.movieapp.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    /** Foydalanuvchi uchun: faqat faol obuna tariflarini ko'rish */
    @GetMapping("/subscription/plans")
    public ResponseEntity<List<SubscriptionPlan>> getActivePlans() {
        return ResponseEntity.ok(subscriptionPlanService.findAllActive());
    }

    /** Admin uchun: faol va nofaol barcha tariflar */
    @GetMapping("/admin/subscription/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        return ResponseEntity.ok(subscriptionPlanService.findAll());
    }

    @PostMapping("/admin/subscription/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPlan> createPlan(@RequestBody SubscriptionPlanDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionPlanService.create(dto));
    }

    @PutMapping("/admin/subscription/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPlan> updatePlan(@PathVariable Long id,
                                                        @RequestBody SubscriptionPlanDto dto) {
        return ResponseEntity.ok(subscriptionPlanService.update(id, dto));
    }

    @DeleteMapping("/admin/subscription/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        subscriptionPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
