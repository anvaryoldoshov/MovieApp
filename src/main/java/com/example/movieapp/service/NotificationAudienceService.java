package com.example.movieapp.service;

import com.example.movieapp.enums.NotificationAudience;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.enums.PaymentType;
import com.example.movieapp.repository.BotPurchaseRepository;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserDeviceRepository;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin push'ni tanlangan seriallarni sotib olganlarga yuborish uchun qabul qiluvchilarni aniqlaydi.
 * Muddati tugagan MovieAccess yozuvlari har kecha o'chiriladi, shuning uchun eski xaridorlar
 * to'lovlar (Payment) va bot xaridlari (BotPurchase) tarixidan topiladi.
 * Umumiy obunaga qo'shilgan (subscriptionBased) serial tanlansa, premium obunachilar ham qo'shiladi.
 */
@Service
@RequiredArgsConstructor
public class NotificationAudienceService {

    private static final int IN_CHUNK = 1000;

    private final MovieAccessRepository movieAccessRepository;
    private final PaymentRepository paymentRepository;
    private final BotPurchaseRepository botPurchaseRepository;
    private final UserRepo userRepo;
    private final SeriesRepo seriesRepo;
    private final UserDeviceRepository userDeviceRepository;

    @Transactional(readOnly = true)
    public Set<Long> resolveUserIds(Collection<Long> seriesIds, NotificationAudience audience) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return Set.of();
        }
        boolean includesPremium = seriesRepo.findAllById(seriesIds).stream()
                .anyMatch(s -> Boolean.TRUE.equals(s.getSubscriptionBased()));

        Set<Long> active = new HashSet<>(movieAccessRepository.findActivePaidUserIdsBySeriesIds(seriesIds));
        if (includesPremium) {
            active.addAll(userRepo.findActiveSubscriberIds());
        }
        if (audience == NotificationAudience.ACTIVE) {
            return active;
        }

        Set<Long> ever = new HashSet<>(active);
        ever.addAll(paymentRepository.findUserIdsBySeriesIdsAndStatus(seriesIds, PaymentStatus.PAID));
        ever.addAll(botPurchaseRepository.findUserIdsBySeriesIds(seriesIds));
        if (includesPremium) {
            ever.addAll(userRepo.findEverSubscribedIds());
            ever.addAll(paymentRepository.findUserIdsByTypeAndStatus(PaymentType.SUBSCRIPTION, PaymentStatus.PAID));
        }
        if (audience == NotificationAudience.EXPIRED) {
            ever.removeAll(active);
        }
        return ever;
    }

    @Transactional(readOnly = true)
    public List<String> findTokens(Collection<Long> userIds) {
        List<Long> ids = new ArrayList<>(userIds);
        Set<String> tokens = new LinkedHashSet<>();
        for (int i = 0; i < ids.size(); i += IN_CHUNK) {
            tokens.addAll(userDeviceRepository.findFcmTokensByUserIds(ids.subList(i, Math.min(i + IN_CHUNK, ids.size()))));
        }
        return new ArrayList<>(tokens);
    }
}
