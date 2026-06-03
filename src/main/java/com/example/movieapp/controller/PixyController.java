package com.example.movieapp.controller;

import com.example.movieapp.entities.Payment;
import com.example.movieapp.service.PaymentService;
import com.example.movieapp.service.PixyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("api/payment/pixy")
@RequiredArgsConstructor
@Tag(name = "Pixy", description = "Pixy to'lov tizimi webhook")
public class PixyController {

    private final PixyService pixyService;
    private final PaymentService paymentService;

    @GetMapping("/callback")
    @Operation(summary = "Pixy webhook callback (to'lov amalga oshganda Pixy chaqiradi)")
    public ResponseEntity<String> callback(
            @RequestParam String status,
            @RequestParam("order_id") String orderId,
            @RequestParam("order_hash") String orderHash,
            @RequestParam Long amount,
            @RequestParam String note
    ) {
        log.info("Pixy webhook: status={}, orderId={}, note={}", status, orderId, note);

        Optional<Payment> opt = pixyService.findAndVerifyPayment(orderHash);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body("Noto'g'ri hash");
        }

        Payment payment = opt.get();

        if (pixyService.isPaid(payment)) {
            return ResponseEntity.ok("OK");
        }

        if ("paid".equalsIgnoreCase(status)) {
            paymentService.activateAccess(payment);
            log.info("Pixy to'lov tasdiqlandi: paymentId={}", payment.getId());
        } else {
            log.warn("Pixy webhook: noma'lum status={}, paymentId={}", status, payment.getId());
        }

        return ResponseEntity.ok("OK");
    }
}
