package com.example.movieapp.service;

import com.example.movieapp.entities.NotificationSound;
import com.example.movieapp.repository.NotificationSoundRepository;
import com.example.movieapp.repository.UserDeviceRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int BATCH_SIZE = 500; // FCM multicast cheklovi
    private static final String DEFAULT_SOUND = "default";

    private final UserDeviceRepository userDeviceRepository;
    private final NotificationSoundRepository notificationSoundRepository;

    @Value("${fcm.android-channel-id}")
    private String androidChannelId;

    /**
     * Admin panel orqali barcha foydalanuvchilarga qo'lda push-notification yuborish.
     */
    public int[] sendCustomNotification(String title, String body, String imageUrl, String sound) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase sozlanmagan, push-notification yuborilmadi");
            return new int[]{0, 0};
        }

        List<String> tokens = userDeviceRepository.findAllFcmTokens();
        if (tokens.isEmpty()) {
            return new int[]{0, 0};
        }

        String soundToUse = (sound == null || sound.isBlank()) ? DEFAULT_SOUND : sound;
        if (sound != null && !sound.isBlank()) {
            rememberSound(sound);
        }

        Notification notification = buildNotification(title, body, imageUrl);
        AndroidConfig androidConfig = buildAndroidConfig(soundToUse);
        ApnsConfig apnsConfig = buildApnsConfig(soundToUse);

        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < tokens.size(); i += BATCH_SIZE) {
            List<String> batch = tokens.subList(i, Math.min(i + BATCH_SIZE, tokens.size()));

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig)
                    .putData("type", "ADMIN_PUSH")
                    .putData("title", title)
                    .putData("body", body)
                    .putData("sound", soundToUse)
                    .addAllTokens(batch)
                    .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();
                log.info("Push-notification yuborildi: {} muvaffaqiyatli, {} xato",
                        response.getSuccessCount(), response.getFailureCount());
            } catch (FirebaseMessagingException e) {
                failureCount += batch.size();
                log.error("Push-notification yuborishda xatolik: {}", e.getMessage());
            }
        }

        return new int[]{successCount, failureCount};
    }

    /**
     * Faqat bitta FCM tokenga test push-notification yuborish (bazadagi userlarga tegmaydi).
     */
    public boolean sendTestNotification(String token, String title, String body, String imageUrl, String sound) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase sozlanmagan, test push-notification yuborilmadi");
            return false;
        }

        String soundToUse = (sound == null || sound.isBlank()) ? DEFAULT_SOUND : sound;

        Message message = Message.builder()
                .setToken(token)
                .setNotification(buildNotification(title, body, imageUrl))
                .setAndroidConfig(buildAndroidConfig(soundToUse))
                .setApnsConfig(buildApnsConfig(soundToUse))
                .putData("type", "ADMIN_TEST_PUSH")
                .putData("title", title)
                .putData("body", body)
                .putData("sound", soundToUse)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("Test push-notification yuborishda xatolik: {}", e.getMessage());
            return false;
        }
    }

    private Notification buildNotification(String title, String body, String imageUrl) {
        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(title)
                .setBody(body);
        if (imageUrl != null && !imageUrl.isBlank()) {
            notificationBuilder.setImage(imageUrl);
        }
        return notificationBuilder.build();
    }

    private AndroidConfig buildAndroidConfig(String sound) {
        return AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setChannelId(androidChannelId)
                        .setSound(sound)
                        .build())
                .build();
    }

    private ApnsConfig buildApnsConfig(String sound) {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setSound(sound).build())
                .build();
    }

    private void rememberSound(String sound) {
        NotificationSound entry = notificationSoundRepository.findByValue(sound)
                .orElseGet(() -> NotificationSound.builder().value(sound).build());
        entry.setLastUsedAt(Instant.now());
        notificationSoundRepository.save(entry);
    }

    public List<String> getRecentSounds() {
        return notificationSoundRepository.findAllByOrderByLastUsedAtDesc()
                .stream()
                .map(NotificationSound::getValue)
                .toList();
    }
}
