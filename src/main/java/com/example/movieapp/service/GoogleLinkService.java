package com.example.movieapp.service;

import com.example.movieapp.entities.User;
import com.example.movieapp.repository.RefreshTokenRepository;
import com.example.movieapp.repository.UserRepo;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleLinkService {

    private static final Logger log = LoggerFactory.getLogger(GoogleLinkService.class);

    private final AuthService authService;
    private final AccountMergeService accountMergeService;
    private final UserRepo userRepo;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public ResponseEntity<?> link(User current, String credential, String deviceId) {
        if (!AccountMergeService.isTelegramOnly(current)) {
            return error(HttpStatus.BAD_REQUEST, "ALREADY_LINKED", "Hisobingizga Google allaqachon ulangan");
        }
        if (credential == null || credential.isBlank() || deviceId == null || deviceId.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "credential va deviceId majburiy");
        }

        GoogleIdToken idToken = authService.verifyGoogleToken(credential);
        if (idToken == null || idToken.getPayload().getEmail() == null) {
            return error(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Google tokeni yaroqsiz");
        }
        String email = idToken.getPayload().getEmail().trim();
        String name = (String) idToken.getPayload().get("name");

        User existing = userRepo.findByEmail(email).orElse(null);
        User result;
        if (existing != null) {
            if (existing.getTelegramChatId() != null && !existing.getTelegramChatId().equals(current.getTelegramChatId())) {
                return error(HttpStatus.CONFLICT, "GOOGLE_IN_USE", "Bu Google akkauntga boshqa Telegram ulangan");
            }
            result = accountMergeService.merge(current.getId(), existing.getId());
        } else {
            User me = userRepo.findById(current.getId()).orElseThrow();
            refreshTokenRepository.deleteByUser(me);
            me.setEmail(email);
            if (me.getUsername() == null || me.getUsername().isBlank()) {
                me.setUsername(name);
            }
            result = userRepo.save(me);
        }

        log.info("Google hisobga ulandi: user={}, email={}", result.getId(), email);
        return ResponseEntity.ok(authService.issueTokens(result, deviceId));
    }

    private ResponseEntity<?> error(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
