package com.example.movieapp.service;

import com.example.movieapp.entities.BotPurchase;
import com.example.movieapp.entities.MovieAccess;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.BotPurchaseStatus;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.repository.BotPurchaseRepository;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.RefreshTokenRepository;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountUnlinkService {

    private static final Logger log = LoggerFactory.getLogger(AccountUnlinkService.class);
    private static final Duration COOLDOWN = Duration.ofHours(24);

    private final UserRepo userRepo;
    private final BotPurchaseRepository botPurchaseRepository;
    private final MovieAccessRepository movieAccessRepository;
    private final PaymentRepository paymentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;

    @Transactional
    public ResponseEntity<?> unlinkTelegram(User current) {
        User me = userRepo.findById(current.getId()).orElseThrow();
        ResponseEntity<?> denied = checkBothLinked(me);
        if (denied != null) return denied;

        Long chatId = me.getTelegramChatId();
        int revoked = revokeBotAccess(me, chatId);

        me.setTelegramChatId(null);
        me.setTelegramUsername(null);
        me.setLastUnlinkAt(Instant.now());
        userRepo.save(me);

        log.info("Telegram hisobdan uzildi: user={}, chatId={}, qaytarilgan kirishlar={}", me.getId(), chatId, revoked);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UNLINKED");
        body.put("revoked", revoked);
        return ResponseEntity.ok(body);
    }

    @Transactional
    public ResponseEntity<?> unlinkGoogle(User current, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "deviceId majburiy");
        }
        User me = userRepo.findById(current.getId()).orElseThrow();
        ResponseEntity<?> denied = checkBothLinked(me);
        if (denied != null) return denied;

        String fakeEmail = "tg" + me.getTelegramChatId() + TelegramLoginService.FAKE_EMAIL_DOMAIN;
        User clash = userRepo.findByEmail(fakeEmail).orElse(null);
        if (clash != null && !clash.getId().equals(me.getId())) {
            log.error("Google uzishda email to'qnashuvi: user={}, email={}, boshqa user={}", me.getId(), fakeEmail, clash.getId());
            return error(HttpStatus.CONFLICT, "CONFLICT", "Google'ni uzib bo'lmadi");
        }

        String oldEmail = me.getEmail();
        refreshTokenRepository.deleteByUser(me);
        me.setEmail(fakeEmail);
        me.setLastUnlinkAt(Instant.now());
        me = userRepo.save(me);

        log.info("Google hisobdan uzildi: user={}, email={} -> {}", me.getId(), oldEmail, fakeEmail);
        return ResponseEntity.ok(authService.issueTokens(me, deviceId));
    }

    private ResponseEntity<?> checkBothLinked(User me) {
        if (me.getTelegramChatId() == null || AccountMergeService.isTelegramOnly(me)) {
            return error(HttpStatus.BAD_REQUEST, "NOT_LINKED", "Hisobda faqat bitta kirish usuli bor");
        }
        Instant last = me.getLastUnlinkAt();
        if (last != null && Instant.now().isBefore(last.plus(COOLDOWN))) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "UNLINK_COOLDOWN");
            body.put("message", "Akkauntni 24 soatda bir marta uzish mumkin");
            body.put("retryAt", last.plus(COOLDOWN).toString());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
        }
        return null;
    }

    private int revokeBotAccess(User me, Long chatId) {
        List<BotPurchase> purchases = botPurchaseRepository.findByTelegramIdAndUser_Id(chatId, me.getId());
        Map<Long, LocalDate> botEndBySeries = new HashMap<>();
        for (BotPurchase p : purchases) {
            if (p.getStatus() == BotPurchaseStatus.APPLIED && p.getSeries() != null && p.getAppliedEndDate() != null) {
                botEndBySeries.merge(p.getSeries().getId(), p.getAppliedEndDate(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
            p.setUser(null);
            p.setSeries(null);
            p.setAppliedEndDate(null);
            p.setAppliedAt(null);
            p.setStatus(BotPurchaseStatus.PENDING_USER);
        }
        botPurchaseRepository.saveAll(purchases);

        int revoked = 0;
        for (Map.Entry<Long, LocalDate> e : botEndBySeries.entrySet()) {
            Long seriesId = e.getKey();
            MovieAccess access = movieAccessRepository
                    .findByUser_IdAndMovie_IdAndPaidIsTrue(me.getId(), seriesId).orElse(null);
            if (access == null) continue;
            LocalDate end = access.getAccessEndDate();
            boolean fromBot = end != null && !end.isAfter(e.getValue());
            if (!fromBot) continue;
            if (paymentRepository.existsByUser_IdAndSeries_IdAndStatus(me.getId(), seriesId, PaymentStatus.PAID)) continue;
            movieAccessRepository.delete(access);
            revoked++;
        }
        return revoked;
    }

    private ResponseEntity<?> error(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
