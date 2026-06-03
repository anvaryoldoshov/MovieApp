package com.example.movieapp.service;

import com.example.movieapp.entities.Payment;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PixyService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    @Value("${pixy.api-key:}")
    private String apiKey;

    @Value("${pixy.api-url:https://pay.pixy.uz/api/create}")
    private String apiUrl;

    @Value("${pixy.callback-base-url:}")
    private String callbackBaseUrl;

    public String createPayment(Payment payment, long amountSom) {
        if (apiKey.isBlank()) {
            log.warn("pixy.api-key konfiguratsiyalanmagan");
            return "";
        }

        String callbackUrl = callbackBaseUrl + "/api/payment/pixy/callback";
        String note = "order-" + payment.getId();

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amountSom);
        body.put("callback_url", callbackUrl);
        body.put("note", note);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            Map<?, ?> responseBody = response.getBody();

            if (responseBody == null || !"success".equals(responseBody.get("status"))) {
                log.error("Pixy to'lov yaratishda xato: {}", responseBody);
                return "";
            }

            String paymentUrl = (String) responseBody.get("payment_url");
            String hash = (String) responseBody.get("hash");

            payment.setPixyHash(hash);
            payment.setPixyOrderId(note);
            paymentRepository.save(payment);

            log.info("Pixy payment yaratildi: orderId={}, hash={}", payment.getId(), hash);
            return paymentUrl != null ? paymentUrl : "";

        } catch (Exception e) {
            log.error("Pixy API ga murojaat xatosi: {}", e.getMessage());
            return "";
        }
    }

    public Optional<Payment> findAndVerifyPayment(String orderHash) {
        Optional<Payment> opt = paymentRepository.findByPixyHash(orderHash);
        if (opt.isEmpty()) {
            log.warn("Pixy webhook: hash topilmadi: {}", orderHash);
        }
        return opt;
    }

    public boolean isPaid(Payment payment) {
        return payment.getStatus() == PaymentStatus.PAID;
    }
}
