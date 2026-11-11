package com.example.movieapp.service;

import com.example.movieapp.dto.AuthResponse;
import com.example.movieapp.entities.TelegramLogin;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.Role;
import com.example.movieapp.enums.TelegramLoginStatus;
import com.example.movieapp.repository.TelegramLoginRepository;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TelegramLoginService {

    public static final String FAKE_EMAIL_DOMAIN = "@telegram.tarixiykinolar.uz";

    public enum CallbackResult { SUCCESS, CANCELLED, EXPIRED, FAILED }

    private static final Logger log = LoggerFactory.getLogger(TelegramLoginService.class);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int CODE_LENGTH = 24;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{" + CODE_LENGTH + "}$");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final SecureRandom random = new SecureRandom();
    private final TelegramLoginRepository telegramLoginRepository;
    private final UserRepo userRepo;
    private final AuthService authService;
    private final BotPurchaseService botPurchaseService;
    private final BotSubscriptionFetcher botSubscriptionFetcher;
    private final TelegramOidcClient oidcClient;
    private final AccountMergeService accountMergeService;

    public boolean isValidCode(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    public ResponseEntity<?> start(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "deviceId majburiy"));
        }
        return createLogin(deviceId, null);
    }

    public ResponseEntity<?> startLink(User current) {
        return createLogin("link:" + current.getId(), current.getId());
    }

    @Transactional
    public ResponseEntity<?> checkLink(String code, User current) {
        TelegramLogin login = isValidCode(code) ? telegramLoginRepository.findByCode(code).orElse(null) : null;
        if (login == null || login.getLinkUserId() == null || !login.getLinkUserId().equals(current.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "NOT_FOUND"));
        }
        if (login.getStatus() == TelegramLoginStatus.USED) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("status", "EXPIRED"));
        }
        if (login.getStatus() == TelegramLoginStatus.PENDING) {
            if (Instant.now().isAfter(login.getExpiresAt())) {
                return ResponseEntity.status(HttpStatus.GONE).body(Map.of("status", "EXPIRED"));
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "PENDING"));
        }

        Long chatId = login.getChatId();
        login.setStatus(TelegramLoginStatus.USED);
        telegramLoginRepository.save(login);

        if (current.getTelegramChatId() != null && !current.getTelegramChatId().equals(chatId)) {
            return conflict("ALREADY_LINKED", "Hisobingizga boshqa Telegram akkaunt ulangan");
        }

        User owner = userRepo.findByTelegramChatId(chatId).orElse(null);
        User target;
        if (owner != null && !owner.getId().equals(current.getId())) {
            if (!AccountMergeService.isTelegramOnly(owner)) {
                return conflict("TELEGRAM_IN_USE", "Bu Telegram akkaunt boshqa hisobga ulangan");
            }
            target = accountMergeService.merge(owner.getId(), current.getId());
        } else {
            current.setTelegramChatId(chatId);
            current.setTelegramUsername(login.getTgUsername());
            target = userRepo.save(current);
        }
        if (login.getTgUsername() != null) {
            target.setTelegramUsername(login.getTgUsername());
            target = userRepo.save(target);
        }

        botSubscriptionFetcher.sync(chatId);
        botPurchaseService.applyPendingForUser(target);
        log.info("Telegram hisobga ulandi: user={}, chatId={}", target.getId(), chatId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "LINKED");
        body.put("telegramChatId", target.getTelegramChatId());
        body.put("telegramUsername", target.getTelegramUsername());
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<?> conflict(String status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private ResponseEntity<?> createLogin(String deviceId, Long linkUserId) {
        if (!oidcClient.isConfigured()) {
            log.error("telegram.oidc.client-id / client-secret / redirect-uri sozlanmagan");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Telegram login sozlanmagan"));
        }

        String code;
        do {
            code = randomCode();
        } while (telegramLoginRepository.existsByCode(code));
        String verifier = randomVerifier();

        Instant now = Instant.now();
        telegramLoginRepository.save(TelegramLogin.builder()
                .code(code)
                .codeVerifier(verifier)
                .linkUserId(linkUserId)
                .deviceId(deviceId)
                .status(TelegramLoginStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plus(TTL))
                .build());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("authUrl", oidcClient.authorizationUrl(code, verifier));
        body.put("expiresIn", TTL.toSeconds());
        return ResponseEntity.ok(body);
    }

    @Transactional
    public CallbackResult callback(String state, String authCode, String error) {
        TelegramLogin login = isValidCode(state) ? telegramLoginRepository.findByCode(state).orElse(null) : null;
        if (login == null || login.getStatus() != TelegramLoginStatus.PENDING) {
            return CallbackResult.EXPIRED;
        }
        if (Instant.now().isAfter(login.getExpiresAt())) {
            return CallbackResult.EXPIRED;
        }
        if (error != null && !error.isBlank()) {
            log.info("Telegram login bekor qilindi: error={}", error);
            return CallbackResult.CANCELLED;
        }
        if (authCode == null || authCode.isBlank() || login.getCodeVerifier() == null) {
            return CallbackResult.FAILED;
        }

        TelegramOidcClient.TelegramIdentity identity;
        try {
            identity = oidcClient.exchange(authCode, login.getCodeVerifier());
        } catch (Exception e) {
            log.error("Telegram OIDC token almashtirish xatosi: {}", e.getMessage());
            return CallbackResult.FAILED;
        }

        login.setStatus(TelegramLoginStatus.CONFIRMED);
        login.setConfirmedAt(Instant.now());
        login.setCodeVerifier(null);
        login.setChatId(identity.getId());
        login.setFirstName(identity.getFirstName());
        login.setLastName(identity.getLastName());
        login.setTgUsername(identity.getUsername());
        telegramLoginRepository.save(login);
        return CallbackResult.SUCCESS;
    }

    @Transactional
    public ResponseEntity<?> check(String code, String deviceId) {
        TelegramLogin login = isValidCode(code) ? telegramLoginRepository.findByCode(code).orElse(null) : null;
        if (login == null || login.getLinkUserId() != null || deviceId == null || !deviceId.equals(login.getDeviceId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "NOT_FOUND"));
        }

        if (login.getStatus() == TelegramLoginStatus.USED) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("status", "EXPIRED"));
        }

        if (login.getStatus() == TelegramLoginStatus.PENDING) {
            if (Instant.now().isAfter(login.getExpiresAt())) {
                return ResponseEntity.status(HttpStatus.GONE).body(Map.of("status", "EXPIRED"));
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "PENDING"));
        }

        User user = findOrCreateUser(login);
        login.setStatus(TelegramLoginStatus.USED);
        telegramLoginRepository.save(login);
        botSubscriptionFetcher.sync(user.getTelegramChatId());
        botPurchaseService.applyPendingForUser(user);

        AuthResponse response = authService.issueTokens(user, deviceId);
        return ResponseEntity.ok(response);
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        int removed = telegramLoginRepository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
        if (removed > 0) {
            log.info("Eskirgan telegram login kodlari o'chirildi: {}", removed);
        }
    }

    private User findOrCreateUser(TelegramLogin login) {
        Long chatId = login.getChatId();
        User user = userRepo.findByTelegramChatId(chatId).orElse(null);
        if (user != null) {
            user.setTelegramUsername(login.getTgUsername());
            return userRepo.save(user);
        }
        return userRepo.save(User.builder()
                .email("tg" + chatId + FAKE_EMAIL_DOMAIN)
                .username(displayName(login))
                .telegramChatId(chatId)
                .telegramUsername(login.getTgUsername())
                .subscription(false)
                .userId(authService.generateUniqueUserId())
                .role(Role.USER)
                .build());
    }

    private String displayName(TelegramLogin login) {
        StringBuilder sb = new StringBuilder();
        if (login.getFirstName() != null) sb.append(login.getFirstName().trim());
        if (login.getLastName() != null && !login.getLastName().isBlank()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(login.getLastName().trim());
        }
        if (sb.isEmpty() && login.getTgUsername() != null) sb.append(login.getTgUsername());
        return sb.isEmpty() ? "Telegram foydalanuvchi" : sb.toString();
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String randomVerifier() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
