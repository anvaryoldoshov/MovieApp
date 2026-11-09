package com.example.movieapp.controller;

import com.example.movieapp.dto.BotPurchaseRequest;
import com.example.movieapp.enums.BotPurchaseStatus;
import com.example.movieapp.service.BotPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TelegramBotIntegrationController {

    private final BotPurchaseService botPurchaseService;

    @PostMapping("/integrations/telegram-bot/purchase")
    public ResponseEntity<Map<String, Object>> purchase(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody(required = false) BotPurchaseRequest request) {
        return botPurchaseService.receive(apiKey, request);
    }

    @GetMapping("/admin/telegram-bot/series-mappings")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> mappings() {
        return botPurchaseService.listMappings();
    }

    @PutMapping("/admin/telegram-bot/series-mappings/{botSeriesId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveMapping(@PathVariable Long botSeriesId, @RequestBody Map<String, Object> body) {
        try {
            Long seriesId = toLong(body.get("seriesId"));
            String botTitle = body.get("botTitle") != null ? body.get("botTitle").toString() : null;
            return ResponseEntity.ok(botPurchaseService.saveMapping(botSeriesId, botTitle, seriesId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/telegram-bot/series-mappings/{botSeriesId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long botSeriesId) {
        botPurchaseService.deleteMapping(botSeriesId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/telegram-bot/purchases")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> purchases(@RequestParam(required = false) BotPurchaseStatus status) {
        return botPurchaseService.listPurchases(status);
    }

    private Long toLong(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Noto'g'ri ID: " + v);
        }
    }
}
