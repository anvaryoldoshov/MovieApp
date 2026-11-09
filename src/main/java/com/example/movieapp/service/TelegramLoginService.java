package com.example.movieapp.service;

import com.example.movieapp.dto.AuthResponse;
import com.example.movieapp.dto.TelegramConfirmRequest;
import com.example.movieapp.entities.TelegramLogin;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.Role;
import com.example.movieapp.enums.TelegramLoginStatus;
import com.example.movieapp.repository.TelegramLoginRepository;
import com.example.movieapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TelegramLoginService {

    public static final String FAKE_EMAIL_DOMAIN = "@telegram.tarixiykinolar.uz";

    private static final Logger log = LoggerFactory.getLogger(TelegramLoginService.class);
    private static final String CODE_PREFIX = "login_";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int CODE_LENGTH = 24;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{" + CODE_LENGTH + "}$");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final SecureRandom random = new SecureRandom();
    private final TelegramLoginRepository telegramLoginRepository;
    private final UserRepo userRepo;
    private final AuthService authService;
    private final BotPurchaseService botPurchaseService;

    @Value("${telegram.login.bot-id:}")
    private String botId;

    @Value("${telegram.login.origin:}")
    private String origin;

    @Value("${telegram.login.return-url:}")
    private String returnUrl;

    @Value("${telegram.login.api-key:}")
    private String apiKey;

    public boolean isValidCode(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    public ResponseEntity<?> start(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "deviceId majburiy"));
        }
        if (isBlank(botId) || isBlank(origin) || isBlank(returnUrl)) {
            log.error("telegram.login.bot-id / origin / return-url sozlanmagan");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Telegram login sozlanmagan"));
        }

        String code;
        do {
            code = randomCode();
        } while (telegramLoginRepository.existsByCode(code));

        Instant now = Instant.now();
        telegramLoginRepository.save(TelegramLogin.builder()
                .code(code)
                .deviceId(deviceId)
                .status(TelegramLoginStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plus(TTL))
                .build());

        String returnTo = UriComponentsBuilder.fromHttpUrl(returnUrl)
                .queryParam("login_code", code)
                .toUriString();
        String authUrl = "https://oauth.telegram.org/auth"
                + "?bot_id=" + URLEncoder.encode(botId, StandardCharsets.UTF_8)
                + "&origin=" + URLEncoder.encode(origin, StandardCharsets.UTF_8)
                + "&return_to=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("authUrl", authUrl);
        body.put("expiresIn", TTL.toSeconds());
        return ResponseEntity.ok(body);
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> confirm(String key, TelegramConfirmRequest request) {
        if (!validApiKey(key)) {
            return reply(HttpStatus.UNAUTHORIZED, false, "Ruxsat yo'q", "UNAUTHORIZED");
        }
        try {
            String code = normalize(request != null ? request.getLoginCode() : null);
            TelegramConfirmRequest.TelegramUser tgUser = request != null ? request.getUser() : null;
            if (!isValidCode(code) || tgUser == null || tgUser.getChatId() == null) {
                return reply(HttpStatus.NOT_FOUND, false, "Mavjud emas", "NOT_FOUND");
            }

            TelegramLogin login = telegramLoginRepository.findByCode(code).orElse(null);
            if (login == null) {
                return reply(HttpStatus.NOT_FOUND, false, "Mavjud emas", "NOT_FOUND");
            }
            if (login.getStatus() != TelegramLoginStatus.PENDING) {
                if (login.getStatus() == TelegramLoginStatus.CONFIRMED && tgUser.getChatId().equals(login.getChatId())) {
                    return reply(HttpStatus.OK, true, "Tasdiqlandi", "SUCCESS");
                }
                return reply(HttpStatus.GONE, false, "Eskirgan", "EXPIRED");
            }
            if (Instant.now().isAfter(login.getExpiresAt())) {
                return reply(HttpStatus.GONE, false, "Eskirgan", "EXPIRED");
            }

            login.setStatus(TelegramLoginStatus.CONFIRMED);
            login.setConfirmedAt(Instant.now());
            login.setChatId(tgUser.getChatId());
            login.setFirstName(tgUser.getFirstName());
            login.setLastName(tgUser.getLastName());
            login.setTgUsername(tgUser.getUsername());
            telegramLoginRepository.save(login);

            int subs = botPurchaseService.receiveLoginSubscriptions(tgUser.getChatId(), tgUser.getSubscriptions());
            if (subs > 0) {
                log.info("Telegram login obunalari qabul qilindi: chatId={}, soni={}", tgUser.getChatId(), subs);
            }

            return reply(HttpStatus.OK, true, "Tasdiqlandi", "SUCCESS");
        } catch (Exception e) {
            log.error("Telegram confirm xatosi", e);
            return reply(HttpStatus.INTERNAL_SERVER_ERROR, false, "Server ichki muammosi", "INTERNAL_ERROR");
        }
    }

    @Transactional
    public ResponseEntity<?> check(String code, String deviceId) {
        TelegramLogin login = isValidCode(code) ? telegramLoginRepository.findByCode(code).orElse(null) : null;
        if (login == null || deviceId == null || !deviceId.equals(login.getDeviceId())) {
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

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        String code = raw.trim();
        if (code.startsWith(CODE_PREFIX)) code = code.substring(CODE_PREFIX.length());
        return code;
    }

    private boolean validApiKey(String key) {
        if (isBlank(apiKey) || key == null) return false;
        return MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                key.getBytes(StandardCharsets.UTF_8));
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private ResponseEntity<Map<String, Object>> reply(HttpStatus status, boolean ok, String message, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", ok);
        body.put("message", message);
        body.put("code", code);
        return ResponseEntity.status(status).body(body);
    }
}
