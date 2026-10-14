package com.example.movieapp.controller;

import com.example.movieapp.dto.payment.CreatePaymentOrderRequest;
import com.example.movieapp.dto.payment.CreatePaymentOrderResponse;
import com.example.movieapp.dto.promo.PromoCodeAdminDto;
import com.example.movieapp.dto.promo.PromoCodeRequest;
import com.example.movieapp.dto.promo.PromoUsageDto;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.PaymentService;
import com.example.movieapp.service.PromoCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Promo code", description = "Promokodlar: tekshirish (ilova) va boshqarish (admin)")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;
    private final PaymentService paymentService;
    private final UserRepo userRepo;

    /** Ilova: promokodni qo'llab, chegirmali narxni ko'rsatish (order yaratilmaydi) */
    @PostMapping("/api/payment/promo/check")
    @Operation(summary = "Promokodni tekshirish", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CreatePaymentOrderResponse> check(@RequestBody CreatePaymentOrderRequest request,
                                                            Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(UserNotFoundException::new);
        return ResponseEntity.ok(paymentService.quote(request, user.getId()));
    }

    @GetMapping("/admin/promo-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromoCodeAdminDto>> getAll() {
        return ResponseEntity.ok(promoCodeService.findAll());
    }

    @PostMapping("/admin/promo-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCodeAdminDto> create(@RequestBody PromoCodeRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promoCodeService.create(request, authentication.getName()));
    }

    @PutMapping("/admin/promo-codes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCodeAdminDto> update(@PathVariable Long id, @RequestBody PromoCodeRequest request) {
        return ResponseEntity.ok(promoCodeService.update(id, request));
    }

    @DeleteMapping("/admin/promo-codes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promoCodeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/promo-codes/{id}/usages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromoUsageDto>> usages(@PathVariable Long id) {
        return ResponseEntity.ok(promoCodeService.usages(id));
    }
}
