package com.example.movieapp.service;

import com.example.movieapp.dto.MySubscriptionDto;
import com.example.movieapp.entities.MovieAccess;
import com.example.movieapp.entities.Payment;
import com.example.movieapp.enums.PaymentProvider;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.enums.PaymentType;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.SubscriptionPlanRepo;
import com.example.movieapp.entities.SubscriptionPlan;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.exception.MovieAccessAlreadyExistsException;
import com.example.movieapp.exception.MovieAccessNotFoundException;
import com.example.movieapp.exception.SeriesNotFoundException;
import com.example.movieapp.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieAccessService {

    private final MovieAccessRepository movieAccessRepository;
    private final UserRepo userRepo;
    private final SeriesRepo seriesRepo;
    private final UserNotificationService userNotificationService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepo subscriptionPlanRepo;

    public MovieAccess giveAccess(Long userId, Long seriesId, boolean paid) {
        User user = userRepo.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        Series series = seriesRepo.findById(seriesId)
                .orElseThrow(SeriesNotFoundException::new);

        if (movieAccessRepository.existsByUserIdAndMovieId(userId, seriesId)) {
            log.error("Movie access already exists for user {} and series {}", userId, seriesId);
            throw new MovieAccessAlreadyExistsException();
        }
        MovieAccess access = new MovieAccess();
        access.setUser(user);
        access.setMovie(series);
        access.setPaid(paid);

        log.debug("Movie access has been given: User {} for Series {}", user.getUsername(), series.getTitle());
        return movieAccessRepository.save(access);
    }

    /**
     * Foydalanuvchining obuna holati (mobil ilova profili uchun).
     */
    public MySubscriptionDto getMySubscription(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(UserNotFoundException::new);
        LocalDate today = LocalDate.now();

        LocalDate subEnd = user.getSubscriptionEndDate();
        boolean active = Boolean.TRUE.equals(user.getSubscription())
                && (subEnd == null || !today.isAfter(subEnd));

        List<MySubscriptionDto.SeriesAccess> series = movieAccessRepository.findByUserIdAndPaidTrue(userId)
                .stream()
                .filter(a -> a.getAccessEndDate() == null || !today.isAfter(a.getAccessEndDate()))
                .map(a -> MySubscriptionDto.SeriesAccess.builder()
                        .seriesId(a.getMovie().getId())
                        .title(a.getMovie().getTitle())
                        .imagePath(a.getMovie().getImagePath())
                        .endDate(a.getAccessEndDate())
                        .daysLeft(daysLeft(today, a.getAccessEndDate()))
                        .build())
                .toList();

        return MySubscriptionDto.builder()
                .active(active)
                .endDate(active ? subEnd : null)
                .daysLeft(active ? daysLeft(today, subEnd) : 0)
                .series(series)
                .build();
    }

    private static long daysLeft(LocalDate today, LocalDate end) {
        return end == null ? 0 : Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, end));
    }

    public List<Series> getUserAccessedSeries(Long userId) {
        log.debug("Getting paid series access for user {}", userId);
        LocalDate today = LocalDate.now();
        return movieAccessRepository.findByUserIdAndPaidTrue(userId)
                .stream()
                .filter(access -> access.getAccessEndDate() == null || !today.isAfter(access.getAccessEndDate()))
                .map(MovieAccess::getMovie)
                .toList();
    }

    public List<MovieAccess> getAllMovies() {
        return movieAccessRepository.findAll();
    }

    @Transactional
    public ResponseEntity<?> deleteAccess(Long userId, Long movieId) {
        Optional<MovieAccess> accessOpt = movieAccessRepository
                .findByUserIdAndMovieIdAndPaidTrue(userId, movieId);

        if (accessOpt.isEmpty()) {
            throw new MovieAccessNotFoundException();
        }

        log.debug("Movie access deleted: User {} for Movie {}", accessOpt.get().getUser().getUsername(), accessOpt.get().getMovie().getTitle());
        cancelActiveManualPayments(userId, movieId);
        movieAccessRepository.delete(accessOpt.get());
        return ResponseEntity.ok("Movie access deleted for userId=" + userId + ", movieId=" + movieId);
    }

    @Transactional
    public void removeExpiredAccesses() {
        LocalDate today = LocalDate.now();
        List<MovieAccess> expired = movieAccessRepository.findByPaidTrueAndAccessEndDateIsNotNullAndAccessEndDateBefore(today);
        if (!expired.isEmpty()) {
            log.info("Removing {} expired movie accesses.", expired.size());
            movieAccessRepository.deleteAll(expired);
        }
    }

    @Transactional
    public void updateUserAccessWithSubscription(
            Long userId,
            Map<Long, Integer> seriesAccessMap,
            boolean hasSubscription,
            Integer subscriptionDays,
            Map<Long, Long> manualPayments,
            String adminEmail
    ) {
        User user = userRepo.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        boolean hadSubscription = Boolean.TRUE.equals(user.getSubscription());
        LocalDate previousSubscriptionEnd = user.getSubscriptionEndDate();
        user.setSubscription(hasSubscription);
        log.info("Updating access for user {}. Subscription: {}, Days: {}", userId, hasSubscription, subscriptionDays);

        if (hasSubscription && subscriptionDays != null && subscriptionDays > 0) {
            // Obuna Yoqilgan
            LocalDate accessEndDate = LocalDate.now().plusDays(subscriptionDays);
            if (!hadSubscription || !accessEndDate.equals(previousSubscriptionEnd)) {
                userNotificationService.notifyUser(
                        user,
                        "SUBSCRIPTION_ACTIVATED",
                        "Obunangiz faollashtirildi!",
                        String.format("%d kunlik obuna faollashtirildi. Tugash sanasi: %s", subscriptionDays, accessEndDate),
                        null
                );
            }
            user.setSubscriptionStartDate(LocalDate.now());
            user.setSubscriptionEndDate(accessEndDate);

            // Barcha seriallarga MovieAccess yozuvlarini qo'shish
            List<Series> allSeries = seriesRepo.findAll();

            for (Series series : allSeries) {
                Optional<MovieAccess> existingAccessOpt = movieAccessRepository
                        .findByUser_IdAndMovie_IdAndPaidIsTrue(userId, series.getId());

                MovieAccess access = existingAccessOpt.orElseGet(MovieAccess::new);

                access.setUser(user);
                access.setMovie(series);
                access.setPaid(true);
                access.setAccessEndDate(accessEndDate); // Obuna muddatini belgilash
                access.setReminderSent(false);

                movieAccessRepository.save(access);
                log.debug("Added/Updated access for series {} (Subscription, End Date: {})", series.getTitle(), accessEndDate);
            }

        } else {
            // Obuna O'chirilgan
            user.setSubscriptionStartDate(null);
            user.setSubscriptionEndDate(null);

            List<MovieAccess> existingPaidAccesses = movieAccessRepository.findByUserAndPaidIsTrue(user);
            Set<Long> updatedSeriesIds = seriesAccessMap.keySet();
            LocalDate now = LocalDate.now();
            List<MovieAccess> grantedAccesses = new ArrayList<>();

            // A. Individual Access yaratish/yangilash
            for (Map.Entry<Long, Integer> entry : seriesAccessMap.entrySet()) {
                Long seriesId = entry.getKey();
                Integer days = entry.getValue();

                if (days == null || days <= 0) continue;

                Series series = seriesRepo.findById(seriesId)
                        .orElseThrow(SeriesNotFoundException::new);

                Optional<MovieAccess> existingAccess = existingPaidAccesses.stream()
                        .filter(ma -> ma.getMovie().getId().equals(seriesId))
                        .findFirst();

                MovieAccess access = existingAccess.orElseGet(MovieAccess::new);

                LocalDate newEndDate = now.plusDays(days);

                access.setUser(user);
                access.setMovie(series);
                access.setPaid(true);
                access.setAccessEndDate(newEndDate);
                access.setReminderSent(false);

                movieAccessRepository.save(access);
                log.debug("Individual access updated for series {}. End Date: {}", series.getTitle(), newEndDate);

                if (manualPayments.containsKey(seriesId)) {
                    recordManualPayment(user, series, days, manualPayments.get(seriesId), adminEmail);
                }

                // Faqat yangi ochilgan yoki "to'lov qildi" bilan uzaytirilgan kirish haqida xabar beriladi,
                // aks holda admin foydalanuvchini har safar saqlaganda takror bildirishnoma ketardi.
                if (existingAccess.isEmpty() || manualPayments.containsKey(seriesId)) {
                    grantedAccesses.add(access);
                }
            }

            notifyAccessGranted(user, grantedAccesses);

            // B. O'chirish
            existingPaidAccesses.stream()
                    .filter(ma -> !updatedSeriesIds.contains(ma.getMovie().getId()))
                    .forEach(access -> {
                        cancelActiveManualPayments(userId, access.getMovie().getId());
                        movieAccessRepository.delete(access);
                        log.debug("Deleted expired/unselected individual access for series {}", access.getMovie().getTitle());
                    });
        }

        userRepo.save(user); // Obuna o'rnatilsa ham, o'chirilmasa ham User obyekti saqlanadi
    }


    private void notifyAccessGranted(User user, List<MovieAccess> grantedAccesses) {
        if (grantedAccesses.isEmpty()) {
            return;
        }
        if (grantedAccesses.size() == 1) {
            MovieAccess access = grantedAccesses.get(0);
            userNotificationService.notifyUser(
                    user,
                    "ACCESS_GRANTED",
                    "Serialga kirish ochildi",
                    String.format("\"%s\" serialini %s kunigacha tomosha qilishingiz mumkin",
                            access.getMovie().getTitle(), access.getAccessEndDate()),
                    null,
                    access.getMovie().getId()
            );
            return;
        }
        LocalDate latestEnd = grantedAccesses.stream()
                .map(MovieAccess::getAccessEndDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        userNotificationService.notifyUser(
                user,
                "ACCESS_GRANTED",
                "Seriallarga kirish ochildi",
                String.format("%d ta serialga kirish ochildi. Tugash sanasi: %s", grantedAccesses.size(), latestEnd),
                null
        );
    }

    /** Admin "to'lov qildi" deb belgilaganda statistikaga tushadigan qo'lda to'lov yoziladi (summa so'mda, komissiyasiz). */
    private void recordManualPayment(User user, Series series, int days, Long amountSom, String adminEmail) {
        long amount = amountSom != null && amountSom > 0 ? amountSom : defaultPrice(series, days);
        paymentRepository.save(Payment.builder()
                .user(user)
                .series(series)
                .amount(amount * 100)
                .status(PaymentStatus.PAID)
                .provider(PaymentProvider.MANUAL)
                .paymentType(PaymentType.INDIVIDUAL_SERIES)
                .subscriptionDays(days)
                .createdBy(adminEmail)
                .build());
        log.info("Qo'lda to'lov yozildi: user={}, series={}, kunlar={}, summa={} so'm, admin={}",
                user.getId(), series.getId(), days, amount, adminEmail);
    }

    /** Obunaga asoslangan serialda narx faol obuna tarifidan, aks holda serialning o'z narxidan olinadi. */
    private long defaultPrice(Series series, int days) {
        Long monthly = series.getMonthlyPrice();
        Long quarterly = series.getQuarterlyPrice();
        if (Boolean.TRUE.equals(series.getSubscriptionBased())) {
            SubscriptionPlan plan = subscriptionPlanRepo.findByActiveTrue().stream().findFirst().orElse(null);
            monthly = plan != null ? plan.getMonthlyPrice() : null;
            quarterly = plan != null ? plan.getQuarterlyPrice() : null;
        }
        if (days >= 90 && quarterly != null) return quarterly;
        return monthly != null ? monthly * Math.max(1, (days + 29) / 30) : 0;
    }

    /** Kirish o'chirilganda (masalan, adashib yoqilgan bo'lsa) muddati hali tugamagan qo'lda to'lovlar bekor qilinadi va statistikadan chiqadi. */
    private void cancelActiveManualPayments(Long userId, Long seriesId) {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> active = paymentRepository
                .findByUser_IdAndSeries_IdAndProviderAndStatus(userId, seriesId, PaymentProvider.MANUAL, PaymentStatus.PAID)
                .stream()
                .filter(p -> p.getCreatedAt() == null || p.getSubscriptionDays() == null
                        || p.getCreatedAt().plusDays(p.getSubscriptionDays()).isAfter(now))
                .toList();
        if (active.isEmpty()) return;
        active.forEach(p -> p.setStatus(PaymentStatus.CANCELLED));
        paymentRepository.saveAll(active);
        log.info("Qo'lda to'lovlar bekor qilindi: user={}, series={}, soni={}", userId, seriesId, active.size());
    }

    @Transactional
    public void grantPaidAccess(Long userId, Long seriesId, int accessDays) {
        User user = userRepo.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        Series series = seriesRepo.findById(seriesId)
                .orElseThrow(SeriesNotFoundException::new);

        MovieAccess access = movieAccessRepository
                .findByUser_IdAndMovie_IdAndPaidIsTrue(userId, seriesId)
                .orElseGet(MovieAccess::new);

        // Muddati hali tugamagan bo'lsa — yangi kunlar qolgan kunlar ustiga qo'shiladi.
        LocalDate today = LocalDate.now();
        LocalDate currentEnd = access.getAccessEndDate();
        LocalDate endDate = (currentEnd != null && !today.isAfter(currentEnd) ? currentEnd : today)
                .plusDays(accessDays);

        access.setUser(user);
        access.setMovie(series);
        access.setPaid(true);
        access.setAccessEndDate(endDate);
        access.setReminderSent(false);
        movieAccessRepository.save(access);

        // Diqqat: user.subscription ga tegmaymiz. Bitta serial sotib olish faqat shu serialni
        // ochadi; umumiy obuna (subscriptionBased seriallar) faqat SubscriptionPlan to'lovi orqali beriladi.

        log.info("Pullik kirish berildi: user={}, series={}, kunlar={}", userId, seriesId, accessDays);
    }

    @Transactional
    public void sendExpiryReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        List<MovieAccess> expiringSoon = movieAccessRepository.findByPaidTrueAndAccessEndDateAndReminderSentFalse(targetDate);

        if (expiringSoon.isEmpty()) {
            return;
        }

        log.info("Obuna tugashiga 3 kun qolgan {} ta kirish uchun eslatma yuborilmoqda.", expiringSoon.size());
        for (MovieAccess access : expiringSoon) {
            userNotificationService.notifyUser(
                    access.getUser(),
                    "SUBSCRIPTION_EXPIRY_REMINDER",
                    "Obunangiz tugashiga 3 kun qoldi",
                    String.format("\"%s\" seriali uchun obunangiz %s kuni tugaydi. Uzaytirishni unutmang!",
                            access.getMovie().getTitle(), access.getAccessEndDate()),
                    null
            );
            access.setReminderSent(true);
        }
        movieAccessRepository.saveAll(expiringSoon);
    }

    public boolean canUserWatchMovie(Long userId, Long serialId) {
        // 1. Individual pullik kirish mavjudmi?
        Optional<MovieAccess> paidAccess = movieAccessRepository.findByUser_IdAndMovie_IdAndPaidIsTrue(userId, serialId);
        if (paidAccess.isPresent()) {
            LocalDate endDate = paidAccess.get().getAccessEndDate();
            if (endDate == null || !LocalDate.now().isAfter(endDate)) {
                log.debug("Access granted for user {} via paid access to serial {}.", userId, serialId);
                return true;
            }
        }

        // 2. Serial obuna orqali ko'rish mumkinmi? (subscriptionBased = true + user faol obunaga ega)
        Series series = seriesRepo.findById(serialId).orElse(null);
        if (series != null && Boolean.TRUE.equals(series.getSubscriptionBased())) {
            User user = userRepo.findById(userId).orElse(null);
            if (user != null && Boolean.TRUE.equals(user.getSubscription())) {
                LocalDate subEnd = user.getSubscriptionEndDate();
                if (subEnd == null || !LocalDate.now().isAfter(subEnd)) {
                    log.debug("Access granted for user {} via active subscription to serial {}.", userId, serialId);
                    return true;
                }
            }
        }

        // 3. Bepul (paid=false) kirish mavjudmi?
        Optional<MovieAccess> freeAccess = movieAccessRepository.findByUser_IdAndMovie_IdAndPaidIsFalse(userId, serialId);
        if (freeAccess.isPresent()) {
            log.debug("Access granted for user {} via free access to serial {}.", userId, serialId);
            return true;
        }

        log.debug("Access denied for user {} to serial {}.", userId, serialId);
        return false;
    }
}
