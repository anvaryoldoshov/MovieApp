package com.example.movieapp.service;

import com.example.movieapp.dto.UserNotificationDto;
import com.example.movieapp.entities.User;
import com.example.movieapp.entities.UserDevice;
import com.example.movieapp.entities.UserNotification;
import com.example.movieapp.exception.UserNotificationNotFoundException;
import com.example.movieapp.mapper.UserNotificationMapper;
import com.example.movieapp.repository.UserDeviceRepository;
import com.example.movieapp.repository.UserNotificationRepository;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private static final int DEFAULT_HISTORY_DAYS = 30;

    private final UserNotificationRepository userNotificationRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationService notificationService;
    private final UserNotificationMapper userNotificationMapper;
    private final UserRepo userRepo;

    /**
     * Notifikatsiyani bazaga (inbox uchun) yozadi va foydalanuvchining qurilmasiga push yuboradi.
     * Push yuborishdagi xatolik yutiladi - bu metod to'lov/cron kabi tranzaksiyalar
     * ichidan chaqirilishi mumkin va push xatosi ularni buzmasligi kerak.
     */
    public void notifyUser(User user, String type, String title, String body, String imageUrl) {
        UserNotification notification = UserNotification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .imageUrl(imageUrl)
                .read(false)
                .build();
        userNotificationRepository.save(notification);

        try {
            userDeviceRepository.findByUserId(user.getId())
                    .map(UserDevice::getFcmToken)
                    .filter(token -> token != null && !token.isBlank())
                    .ifPresent(token -> notificationService.sendToToken(token, type, title, body, imageUrl));
        } catch (Exception e) {
            log.error("Foydalanuvchi {} ga push yuborishda xatolik: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * Admin barcha foydalanuvchilarga yuborgan push'ni har birining inbox'iga yozadi
     * (push'ning o'zi NotificationService.sendCustomNotification orqali alohida yuboriladi).
     * Shunda foydalanuvchi bildirishnomani o'chirib yuborsa ham, ilova ichida ko'ra oladi.
     */
    @Transactional
    public int saveBroadcastToInbox(String type, String title, String body, String imageUrl) {
        String safeBody = body != null && body.length() > 1000 ? body.substring(0, 1000) : body;
        List<UserNotification> rows = userRepo.findAllIds().stream()
                .map(id -> UserNotification.builder()
                        .user(userRepo.getReferenceById(id))
                        .type(type)
                        .title(title)
                        .body(safeBody)
                        .imageUrl(imageUrl)
                        .read(false)
                        .build())
                .toList();
        userNotificationRepository.saveAll(rows);
        return rows.size();
    }

    public List<UserNotificationDto> getRecent(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days > 0 ? days : DEFAULT_HISTORY_DAYS);
        return userNotificationRepository.findByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since)
                .stream()
                .map(userNotificationMapper::toDto)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(DEFAULT_HISTORY_DAYS);
        return userNotificationRepository.countByUser_IdAndReadFalseAndCreatedAtAfter(userId, since);
    }

    public void markAsRead(Long userId, Long notificationId) {
        UserNotification notification = userNotificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(UserNotificationNotFoundException::new);
        notification.setRead(true);
        userNotificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(DEFAULT_HISTORY_DAYS);
        List<UserNotification> unread = userNotificationRepository.findByUser_IdAndReadFalseAndCreatedAtAfter(userId, since);
        unread.forEach(n -> n.setRead(true));
        userNotificationRepository.saveAll(unread);
    }
}
