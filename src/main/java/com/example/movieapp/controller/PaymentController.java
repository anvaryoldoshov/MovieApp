package com.example.movieapp.controller;

import com.example.movieapp.dto.payment.CreatePaymentOrderRequest;
import com.example.movieapp.dto.payment.CreatePaymentOrderResponse;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "To'lov orderlari yaratish")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepo userRepo;

    @PostMapping("/create")
    @Operation(summary = "To'lov order yaratish", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CreatePaymentOrderResponse> createOrder(
            @RequestBody CreatePaymentOrderRequest request,
            Authentication authentication
    ) {
        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(UserNotFoundException::new);
        return ResponseEntity.ok(paymentService.createOrder(request, user.getId()));
    }
}
