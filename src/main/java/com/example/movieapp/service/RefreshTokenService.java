package com.example.movieapp.service;

import com.example.movieapp.entities.RefreshToken;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.SessionExpiredException;
import com.example.movieapp.exception.SessionNotFoundException;
import com.example.movieapp.repository.RefreshTokenRepository;
import com.example.movieapp.repository.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepo userRepo;

    @Value("${jwt.refresh-token-expiry-ms:2592000000}")
    private long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public void validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(SessionNotFoundException::new);

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new SessionExpiredException();
        }

    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public String getEmailFromToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(SessionNotFoundException::new);
        return refreshToken.getUser().getEmail();
    }
}
