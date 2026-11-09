package com.example.movieapp.service;

import com.example.movieapp.dto.BotPurchaseRequest;
import com.example.movieapp.dto.TelegramConfirmRequest;
import com.example.movieapp.entities.BotPurchase;
import com.example.movieapp.entities.BotSeriesMapping;
import com.example.movieapp.entities.MovieAccess;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.BotPurchaseStatus;
import com.example.movieapp.repository.BotPurchaseRepository;
import com.example.movieapp.repository.BotSeriesMappingRepository;
import com.example.movieapp.repository.MovieAccessRepository;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BotPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(BotPurchaseService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final List<BotPurchaseStatus> PENDING =
            List.of(BotPurchaseStatus.PENDING_USER, BotPurchaseStatus.PENDING_MAPPING);

    private final BotPurchaseRepository botPurchaseRepository;
    private final BotSeriesMappingRepository mappingRepository;
    private final MovieAccessRepository movieAccessRepository;
    private final SeriesRepo seriesRepo;
    private final UserRepo userRepo;
    private final UserNotificationService userNotificationService;

    @Value("${telegram.login.api-key:}")
    private String apiKey;

    @Transactional
    public ResponseEntity<Map<String, Object>> receive(String key, BotPurchaseRequest req) {
        if (!validApiKey(key)) {
            return reply(HttpStatus.UNAUTHORIZED, false, "Ruxsat yo'q", "UNAUTHORIZED");
        }
        return ingest(req);
    }

    @Transactional
    public int receiveLoginSubscriptions(Long chatId, List<TelegramConfirmRequest.Subscription> subscriptions) {
        if (chatId == null || subscriptions == null || subscriptions.isEmpty()) return 0;
        int saved = 0;
        for (TelegramConfirmRequest.Subscription s : subscriptions) {
            if (s == null || s.getSeriesId() == null) continue;
            if (isBlank(s.getExpiresAt()) && (s.getPlanDays() == null || s.getPlanDays() <= 0)) continue;
            BotPurchaseRequest req = new BotPurchaseRequest();
            req.setPurchaseId("login:" + chatId + ":" + s.getSeriesId() + ":"
                    + (isBlank(s.getExpiresAt()) ? s.getPurchasedAt() + ":" + s.getPlanDays() : s.getExpiresAt().trim()));
            req.setTelegramId(chatId);
            req.setSeriesId(s.getSeriesId());
            req.setTitle(s.getTitle());
            req.setPlanDays(s.getPlanDays());
            req.setPurchasedAt(s.getPurchasedAt());
            req.setExpiresAt(s.getExpiresAt());
            req.setSource(s.getSource());
            ResponseEntity<Map<String, Object>> res = ingest(req);
            if (res.getStatusCode().is2xxSuccessful()) {
                saved++;
            } else {
                log.warn("Login obunasi qabul qilinmadi: chatId={}, series={}, body={}", chatId, s.getSeriesId(), res.getBody());
            }
        }
        return saved;
    }

    private ResponseEntity<Map<String, Object>> ingest(BotPurchaseRequest req) {
        if (req == null || isBlank(req.getPurchaseId()) || req.getTelegramId() == null || req.getSeriesId() == null) {
            return reply(HttpStatus.BAD_REQUEST, false, "purchase_id, telegram_id, series_id majburiy", "BAD_REQUEST");
        }

        LocalDate expiresAt;
        Instant purchasedAt;
        try {
            expiresAt = parseDate(req.getExpiresAt());
            purchasedAt = parseInstant(req.getPurchasedAt());
        } catch (DateTimeParseException e) {
            return reply(HttpStatus.BAD_REQUEST, false, "Sana formati noto'g'ri", "BAD_REQUEST");
        }
        if (expiresAt == null && (req.getPlanDays() == null || req.getPlanDays() <= 0)) {
            return reply(HttpStatus.BAD_REQUEST, false, "expires_at yoki plan_days majburiy", "BAD_REQUEST");
        }

        String purchaseId = req.getPurchaseId().trim();
        if (botPurchaseRepository.existsByPurchaseId(purchaseId)) {
            return reply(HttpStatus.OK, true, "Avval qabul qilingan", "DUPLICATE");
        }

        BotSeriesMapping mapping = upsertMapping(req.getSeriesId(), req.getTitle());

        BotPurchase purchase = BotPurchase.builder()
                .purchaseId(purchaseId)
                .telegramId(req.getTelegramId())
                .botSeriesId(req.getSeriesId())
                .title(req.getTitle())
                .planDays(req.getPlanDays())
                .purchasedAt(purchasedAt)
                .expiresAt(expiresAt)
                .status(BotPurchaseStatus.PENDING_USER)
                .receivedAt(Instant.now())
                .build();

        purchase = botPurchaseRepository.save(purchase);

        User user = userRepo.findByTelegramChatId(req.getTelegramId()).orElse(null);
        BotPurchaseStatus status = apply(purchase, user, mapping, true);

        return switch (status) {
            case APPLIED -> reply(HttpStatus.OK, true, "Tasdiqlandi", "SUCCESS");
            case PENDING_USER -> reply(HttpStatus.OK, true, "Saqlandi, foydalanuvchi ilovaga kirganda ochiladi", "PENDING_USER");
            case PENDING_MAPPING -> reply(HttpStatus.OK, true, "Saqlandi, serial hali bog'lanmagan", "PENDING_MAPPING");
            case EXPIRED -> reply(HttpStatus.OK, true, "Saqlandi, muddati allaqachon tugagan", "EXPIRED");
        };
    }

    @Transactional
    public int applyPendingForUser(User user) {
        if (user == null || user.getTelegramChatId() == null) return 0;
        List<BotPurchase> pending = botPurchaseRepository.findByTelegramIdAndStatusIn(user.getTelegramChatId(), PENDING);
        int applied = 0;
        for (BotPurchase p : pending) {
            BotSeriesMapping mapping = mappingRepository.findByBotSeriesId(p.getBotSeriesId()).orElse(null);
            if (apply(p, user, mapping, true) == BotPurchaseStatus.APPLIED) applied++;
        }
        return applied;
    }

    @Transactional
    public int applyPendingForMapping(BotSeriesMapping mapping) {
        if (mapping.getSeries() == null) return 0;
        List<BotPurchase> pending = botPurchaseRepository.findByBotSeriesIdAndStatus(
                mapping.getBotSeriesId(), BotPurchaseStatus.PENDING_MAPPING);
        int applied = 0;
        for (BotPurchase p : pending) {
            User user = userRepo.findByTelegramChatId(p.getTelegramId()).orElse(null);
            if (apply(p, user, mapping, true) == BotPurchaseStatus.APPLIED) applied++;
        }
        return applied;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMappings() {
        Map<Long, Long> pendingCounts = new HashMap<>();
        for (Object[] row : botPurchaseRepository.countPendingMappingByBotSeries()) {
            pendingCounts.put((Long) row[0], (Long) row[1]);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (BotSeriesMapping m : mappingRepository.findAllByOrderByBotSeriesIdAsc()) {
            result.add(mappingDto(m, pendingCounts.getOrDefault(m.getBotSeriesId(), 0L)));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> saveMapping(Long botSeriesId, String botTitle, Long seriesId) {
        if (botSeriesId == null) throw new IllegalArgumentException("botSeriesId majburiy");
        BotSeriesMapping mapping = mappingRepository.findByBotSeriesId(botSeriesId)
                .orElseGet(() -> BotSeriesMapping.builder()
                        .botSeriesId(botSeriesId)
                        .createdAt(Instant.now())
                        .build());
        if (botTitle != null && !botTitle.isBlank()) mapping.setBotTitle(botTitle.trim());
        if (seriesId != null) {
            Series series = seriesRepo.findById(seriesId)
                    .orElseThrow(() -> new IllegalArgumentException("Serial topilmadi"));
            mapping.setSeries(series);
        } else {
            mapping.setSeries(null);
        }
        mapping.setAutoMatched(false);
        mapping.setUpdatedAt(Instant.now());
        mapping = mappingRepository.save(mapping);

        int applied = applyPendingForMapping(mapping);
        Map<String, Object> dto = mappingDto(mapping, 0L);
        dto.put("appliedNow", applied);
        return dto;
    }

    @Transactional
    public void deleteMapping(Long botSeriesId) {
        mappingRepository.findByBotSeriesId(botSeriesId).ifPresent(mappingRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPurchases(BotPurchaseStatus status) {
        List<BotPurchase> list = status == null
                ? botPurchaseRepository.findTop200ByOrderByReceivedAtDesc()
                : botPurchaseRepository.findTop200ByStatusOrderByReceivedAtDesc(status);
        List<Map<String, Object>> result = new ArrayList<>();
        for (BotPurchase p : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("purchaseId", p.getPurchaseId());
            m.put("telegramId", p.getTelegramId());
            m.put("botSeriesId", p.getBotSeriesId());
            m.put("title", p.getTitle());
            m.put("planDays", p.getPlanDays());
            m.put("purchasedAt", p.getPurchasedAt());
            m.put("expiresAt", p.getExpiresAt());
            m.put("status", p.getStatus());
            m.put("userId", p.getUser() != null ? p.getUser().getId() : null);
            m.put("username", p.getUser() != null ? p.getUser().getUsername() : null);
            m.put("seriesId", p.getSeries() != null ? p.getSeries().getId() : null);
            m.put("seriesTitle", p.getSeries() != null ? p.getSeries().getTitle() : null);
            m.put("appliedEndDate", p.getAppliedEndDate());
            m.put("appliedAt", p.getAppliedAt());
            m.put("receivedAt", p.getReceivedAt());
            result.add(m);
        }
        return result;
    }

    private BotPurchaseStatus apply(BotPurchase purchase, User user, BotSeriesMapping mapping, boolean notify) {
        if (user == null) {
            return mark(purchase, BotPurchaseStatus.PENDING_USER);
        }
        purchase.setUser(user);
        if (mapping == null || mapping.getSeries() == null) {
            return mark(purchase, BotPurchaseStatus.PENDING_MAPPING);
        }

        Series series = mapping.getSeries();
        LocalDate today = LocalDate.now(ZONE);

        MovieAccess access = movieAccessRepository
                .findByUser_IdAndMovie_IdAndPaidIsTrue(user.getId(), series.getId())
                .orElseGet(MovieAccess::new);
        LocalDate currentEnd = access.getAccessEndDate();
        boolean currentActive = currentEnd != null && !today.isAfter(currentEnd);

        LocalDate newEnd;
        if (purchase.getExpiresAt() != null) {
            newEnd = currentActive && currentEnd.isAfter(purchase.getExpiresAt()) ? currentEnd : purchase.getExpiresAt();
        } else {
            newEnd = (currentActive ? currentEnd : today).plusDays(purchase.getPlanDays());
        }

        purchase.setSeries(series);
        if (newEnd.isBefore(today)) {
            return mark(purchase, BotPurchaseStatus.EXPIRED);
        }

        access.setUser(user);
        access.setMovie(series);
        access.setPaid(true);
        access.setAccessEndDate(newEnd);
        access.setReminderSent(false);
        movieAccessRepository.save(access);

        purchase.setAppliedEndDate(newEnd);
        purchase.setAppliedAt(Instant.now());
        mark(purchase, BotPurchaseStatus.APPLIED);

        log.info("Bot xaridi qo'llandi: purchase={}, user={}, series={}, end={}",
                purchase.getPurchaseId(), user.getId(), series.getId(), newEnd);

        if (notify) {
            try {
                userNotificationService.notifyUser(
                        user,
                        "ACCESS_GRANTED",
                        "Serialga kirish ochildi",
                        String.format("Telegram botdagi xaridingiz: \"%s\" serialini %s kunigacha tomosha qilishingiz mumkin",
                                series.getTitle(), newEnd),
                        null,
                        series.getId());
            } catch (Exception e) {
                log.warn("Bot xaridi bildirishnomasi yuborilmadi: {}", e.getMessage());
            }
        }
        return BotPurchaseStatus.APPLIED;
    }

    private BotPurchaseStatus mark(BotPurchase purchase, BotPurchaseStatus status) {
        purchase.setStatus(status);
        botPurchaseRepository.save(purchase);
        return status;
    }

    private BotSeriesMapping upsertMapping(Long botSeriesId, String title) {
        BotSeriesMapping mapping = mappingRepository.findByBotSeriesId(botSeriesId).orElse(null);
        if (mapping == null) {
            mapping = mappingRepository.save(BotSeriesMapping.builder()
                    .botSeriesId(botSeriesId)
                    .botTitle(title)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        } else if (title != null && !title.isBlank() && !title.equals(mapping.getBotTitle())) {
            mapping.setBotTitle(title);
            mapping.setUpdatedAt(Instant.now());
            mapping = mappingRepository.save(mapping);
        }
        if (mapping.getSeries() == null && mapping.getAutoMatched() == null) {
            mapping = tryAutoMatch(mapping);
        }
        return mapping;
    }

    private BotSeriesMapping tryAutoMatch(BotSeriesMapping mapping) {
        String key = normalizeTitle(mapping.getBotTitle());
        if (key.isEmpty()) return mapping;

        List<Series> matches = new ArrayList<>();
        for (Series s : seriesRepo.findAll()) {
            if (key.equals(normalizeTitle(s.getTitle()))) matches.add(s);
        }
        if (matches.size() != 1) {
            if (matches.size() > 1) {
                log.info("Bot seriali avtomatik bog'lanmadi, bir nechta mos nom: botSeriesId={}, title={}, soni={}",
                        mapping.getBotSeriesId(), mapping.getBotTitle(), matches.size());
            }
            return mapping;
        }

        Series series = matches.get(0);
        mapping.setSeries(series);
        mapping.setAutoMatched(true);
        mapping.setUpdatedAt(Instant.now());
        mapping = mappingRepository.save(mapping);
        log.info("Bot seriali nom bo'yicha avtomatik bog'landi: botSeriesId={} -> seriesId={} ({})",
                mapping.getBotSeriesId(), series.getId(), series.getTitle());
        applyPendingForMapping(mapping);
        return mapping;
    }

    static String normalizeTitle(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        s = s.replaceAll("[ʻʼ'‘’`´]", "");
        s = s.replaceAll("[^\\p{L}\\p{N}]+", " ");
        return s.trim().replaceAll("\\s+", " ");
    }

    private Map<String, Object> mappingDto(BotSeriesMapping m, long pendingCount) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", m.getId());
        dto.put("botSeriesId", m.getBotSeriesId());
        dto.put("botTitle", m.getBotTitle());
        dto.put("seriesId", m.getSeries() != null ? m.getSeries().getId() : null);
        dto.put("seriesTitle", m.getSeries() != null ? m.getSeries().getTitle() : null);
        dto.put("autoMatched", Boolean.TRUE.equals(m.getAutoMatched()));
        dto.put("pendingCount", pendingCount);
        dto.put("updatedAt", m.getUpdatedAt());
        return dto;
    }

    private LocalDate parseDate(String raw) {
        if (isBlank(raw)) return null;
        String v = raw.trim();
        if (v.length() == 10) return LocalDate.parse(v);
        return OffsetDateTime.parse(v).atZoneSameInstant(ZONE).toLocalDate();
    }

    private Instant parseInstant(String raw) {
        if (isBlank(raw)) return null;
        String v = raw.trim();
        if (v.length() == 10) return LocalDate.parse(v).atStartOfDay(ZONE).toInstant();
        return OffsetDateTime.parse(v).toInstant();
    }

    private boolean validApiKey(String key) {
        if (isBlank(apiKey) || key == null) return false;
        return MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private ResponseEntity<Map<String, Object>> reply(HttpStatus status, boolean ok, String message, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", ok);
        body.put("message", message);
        body.put("code", code);
        return ResponseEntity.status(status).body(body);
    }
}
