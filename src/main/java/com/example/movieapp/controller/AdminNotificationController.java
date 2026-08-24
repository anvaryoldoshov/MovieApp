package com.example.movieapp.controller;

import com.example.movieapp.service.FileStorageService;
import com.example.movieapp.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url}")
    private String baseUrl;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/push")
    public ResponseEntity<?> pushNotification(
            @RequestParam("title") String title,
            @RequestParam("body") String body,
            @RequestParam(value = "sound", required = false) String sound,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "data", required = false) String dataJson
    ) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "title va body majburiy"));
        }

        Map<String, String> extraData;
        try {
            extraData = parseExtraData(dataJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "data maydoni noto'g'ri JSON formatda"));
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            String imagePath = fileStorageService.saveImage("notifications", image);
            imageUrl = baseUrl + imagePath;
        }

        int[] result = notificationService.sendCustomNotification(title, body, imageUrl, sound, extraData);

        return ResponseEntity.ok(Map.of(
                "message", "Push-notification yuborildi",
                "success", result[0],
                "failed", result[1]
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/test-push")
    public ResponseEntity<?> testPushNotification(
            @RequestParam("token") String token,
            @RequestParam("title") String title,
            @RequestParam("body") String body,
            @RequestParam(value = "sound", required = false) String sound,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "data", required = false) String dataJson
    ) {
        if (token == null || token.isBlank() || title == null || title.isBlank() || body == null || body.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "token, title va body majburiy"));
        }

        Map<String, String> extraData;
        try {
            extraData = parseExtraData(dataJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "data maydoni noto'g'ri JSON formatda"));
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            String imagePath = fileStorageService.saveImage("notifications", image);
            imageUrl = baseUrl + imagePath;
        }

        boolean sent = notificationService.sendTestNotification(token, title, body, imageUrl, sound, extraData);

        if (!sent) {
            return ResponseEntity.badRequest().body(Map.of("message", "Test push yuborilmadi. Token noto'g'ri yoki Firebase sozlanmagan bo'lishi mumkin."));
        }
        return ResponseEntity.ok(Map.of("message", "Test push yuborildi"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sounds")
    public ResponseEntity<List<String>> getRecentSounds() {
        return ResponseEntity.ok(notificationService.getRecentSounds());
    }

    private Map<String, String> parseExtraData(String dataJson) throws Exception {
        if (dataJson == null || dataJson.isBlank()) {
            return null;
        }
        return objectMapper.readValue(dataJson, new TypeReference<Map<String, String>>() {});
    }
}
