package com.example.movieapp.controller;

import com.example.movieapp.enums.NotificationAudience;
import com.example.movieapp.service.FileStorageService;
import com.example.movieapp.service.NotificationAudienceService;
import com.example.movieapp.service.NotificationService;
import com.example.movieapp.service.UserNotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private static final int NOTIFICATION_IMAGE_MAX_DIMENSION = 1024; // px
    private static final float NOTIFICATION_IMAGE_QUALITY = 0.8f;

    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final NotificationAudienceService notificationAudienceService;
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
            @RequestParam(value = "data", required = false) String dataJson,
            @RequestParam(value = "seriesIds", required = false) List<Long> seriesIds,
            @RequestParam(value = "audience", required = false, defaultValue = "ACTIVE") NotificationAudience audience
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

        boolean targeted = seriesIds != null && !seriesIds.isEmpty();
        Set<Long> targetUserIds = null;
        if (targeted) {
            targetUserIds = notificationAudienceService.resolveUserIds(seriesIds, audience);
            if (targetUserIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tanlangan seriallar bo'yicha foydalanuvchi topilmadi"));
            }
            if (seriesIds.size() == 1 && parseSeriesId(extraData) == null) {
                extraData = extraData == null ? new HashMap<>() : new HashMap<>(extraData);
                extraData.put("seriesId", seriesIds.get(0).toString());
            }
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            String imagePath = fileStorageService.saveCompressedImage(
                    "notifications", image, NOTIFICATION_IMAGE_MAX_DIMENSION, NOTIFICATION_IMAGE_QUALITY);
            imageUrl = baseUrl + imagePath;
        }

        // Ilova ichidagi bildirishnomalar ro'yxatiga (inbox) ham yozamiz — aks holda foydalanuvchi
        // push'ni o'chirib yuborsa, uni qayta topa olmaydi. Xato bo'lsa ham push yuborilaveradi.
        int savedToInbox = 0;
        try {
            savedToInbox = targeted
                    ? userNotificationService.saveToInbox(targetUserIds, "ADMIN_PUSH", title, body, imageUrl,
                            parseSeriesId(extraData))
                    : userNotificationService.saveBroadcastToInbox("ADMIN_PUSH", title, body, imageUrl,
                            parseSeriesId(extraData));
        } catch (Exception e) {
            log.error("Admin push inbox'ga yozilmadi: {}", e.getMessage());
        }

        int[] result = targeted
                ? notificationService.sendCustomNotificationToTokens(
                        notificationAudienceService.findTokens(targetUserIds), title, body, imageUrl, sound, extraData)
                : notificationService.sendCustomNotification(title, body, imageUrl, sound, extraData);

        return ResponseEntity.ok(Map.of(
                "message", "Push-notification yuborildi",
                "success", result[0],
                "failed", result[1],
                "savedToInbox", savedToInbox,
                "targetUsers", targeted ? targetUserIds.size() : savedToInbox
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audience-count")
    public ResponseEntity<?> audienceCount(
            @RequestParam("seriesIds") List<Long> seriesIds,
            @RequestParam(value = "audience", required = false, defaultValue = "ACTIVE") NotificationAudience audience
    ) {
        Set<Long> userIds = notificationAudienceService.resolveUserIds(seriesIds, audience);
        return ResponseEntity.ok(Map.of(
                "users", userIds.size(),
                "devices", userIds.isEmpty() ? 0 : notificationAudienceService.findTokens(userIds).size()
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
            String imagePath = fileStorageService.saveCompressedImage(
                    "notifications", image, NOTIFICATION_IMAGE_MAX_DIMENSION, NOTIFICATION_IMAGE_QUALITY);
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

    // Admin "data" ichida {"seriesId": "30"} bersa, ilovada bildirishnoma bosilganda shu serial ochiladi.
    private static Long parseSeriesId(Map<String, String> extraData) {
        if (extraData == null) return null;
        try {
            String v = extraData.get("seriesId");
            return v == null || v.isBlank() ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, String> parseExtraData(String dataJson) throws Exception {
        if (dataJson == null || dataJson.isBlank()) {
            return null;
        }
        return objectMapper.readValue(dataJson, new TypeReference<Map<String, String>>() {});
    }
}
