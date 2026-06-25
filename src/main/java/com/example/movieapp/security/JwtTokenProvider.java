package com.example.movieapp.security;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import com.example.movieapp.entities.User;
import com.example.movieapp.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    private final long accessTokenValidity = 30L * 24 * 3600_000;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    public String generateToken(String email, long validityMillis, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generateToken(String email, long validityMillis) {
        return generateToken(email, validityMillis, Role.USER);
    }

    public String generateAccessToken(String email) {
        return generateToken(email, accessTokenValidity, Role.USER);
    }

    public String generateAccessToken(String email, Role role) {
        return generateToken(email, accessTokenValidity, role);
    }

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String generateToken(User user) {
        return generateAccessToken(user.getEmail(), user.getRole());
    }

}
