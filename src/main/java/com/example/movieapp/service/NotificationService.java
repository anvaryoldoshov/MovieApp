package com.example.movieapp.service;

import com.example.movieapp.entities.Series;
import com.example.movieapp.repository.UserDeviceRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int BATCH_SIZE = 500; // FCM multicast cheklovi

    private final UserDeviceRepository userDeviceRepository;

    public void sendNewSeriesNotification(Series series) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase sozlanmagan, push-notification yuborilmadi");
            return;
        }

        List<String> tokens = userDeviceRepository.findAllFcmTokens();
        if (tokens.isEmpty()) {
            return;
        }

        Notification notification = Notification.builder()
                .setTitle("Yangi serial qo'shildi!")
                .setBody(series.getTitle())
                .build();

        for (int i = 0; i < tokens.size(); i += BATCH_SIZE) {
            List<String> batch = tokens.subList(i, Math.min(i + BATCH_SIZE, tokens.size()));

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(notification)
                    .putData("type", "NEW_SERIES")
                    .putData("seriesId", String.valueOf(series.getId()))
                    .addAllTokens(batch)
                    .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info("Push-notification yuborildi: {} muvaffaqiyatli, {} xato",
                        response.getSuccessCount(), response.getFailureCount());
            } catch (FirebaseMessagingException e) {
                log.error("Push-notification yuborishda xatolik: {}", e.getMessage());
            }
        }
    }
}
