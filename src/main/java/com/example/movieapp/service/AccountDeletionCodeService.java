package com.example.movieapp.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountDeletionCodeService {

    private record CodeEntry(String code, Instant expiresAt) {
    }

    private static final long EXPIRY_MINUTES = 10;

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String generateCode(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(email.toLowerCase(), new CodeEntry(code, Instant.now().plusSeconds(EXPIRY_MINUTES * 60)));
        return code;
    }

    public boolean verifyCode(String email, String code) {
        CodeEntry entry = codes.get(email.toLowerCase());
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            codes.remove(email.toLowerCase());
            return false;
        }
        boolean matches = entry.code().equals(code);
        if (matches) {
            codes.remove(email.toLowerCase());
        }
        return matches;
    }
}
