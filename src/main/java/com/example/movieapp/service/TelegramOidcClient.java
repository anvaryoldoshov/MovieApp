package com.example.movieapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelegramOidcClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramOidcClient.class);
    private static final String ISSUER = "https://oauth.telegram.org";
    private static final String AUTH_URL = ISSUER + "/auth";
    private static final String TOKEN_URL = ISSUER + "/token";
    private static final String JWKS_URL = ISSUER + "/.well-known/jwks.json";
    private static final String SCOPE = "openid profile";
    private static final Duration JWKS_MIN_REFRESH = Duration.ofMinutes(1);

    private final RestTemplate http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, PublicKey> keys = new ConcurrentHashMap<>();
    private volatile Instant keysFetchedAt = Instant.EPOCH;

    @Value("${telegram.oidc.client-id:}")
    private String clientId;

    @Value("${telegram.oidc.client-secret:}")
    private String clientSecret;

    @Value("${telegram.oidc.redirect-uri:}")
    private String redirectUri;

    public TelegramOidcClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.http = new RestTemplate(factory);
        this.http.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
    }

    public boolean isConfigured() {
        return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri);
    }

    public String authorizationUrl(String state, String codeVerifier) {
        return AUTH_URL
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc(SCOPE)
                + "&state=" + enc(state)
                + "&code_challenge=" + enc(codeChallenge(codeVerifier))
                + "&code_challenge_method=S256";
    }

    public TelegramIdentity exchange(String code, String codeVerifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(clientId, clientSecret, StandardCharsets.UTF_8);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", clientId);
        form.add("code_verifier", codeVerifier);

        ResponseEntity<String> res = http.exchange(TOKEN_URL, HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
        String raw = res.getBody();
        Map<?, ?> body = parseJson(raw);
        Object idToken = body != null ? body.get("id_token") : null;
        if (!(idToken instanceof String token) || token.isBlank()) {
            throw new IllegalStateException("Telegram javobida id_token yo'q: status=" + res.getStatusCode().value() + ", body=" + redact(raw));
        }
        return verify(token);
    }

    private TelegramIdentity verify(String idToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                    @Override
                    public Key resolveSigningKey(JwsHeader header, Claims c) {
                        return key(header.getKeyId());
                    }
                })
                .requireIssuer(ISSUER)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(idToken)
                .getBody();

        if (!audienceMatches(claims.get("aud"))) {
            throw new IllegalStateException("id_token aud mos emas: " + claims.get("aud"));
        }
        if (claims.getExpiration() == null) {
            throw new IllegalStateException("id_token exp yo'q");
        }

        Long id = toLong(claims.get("id"));
        if (id == null) {
            throw new IllegalStateException("id_token ichida Telegram id yo'q");
        }

        return TelegramIdentity.builder()
                .id(id)
                .firstName(str(claims.get("given_name")) != null ? str(claims.get("given_name")) : str(claims.get("name")))
                .lastName(str(claims.get("family_name")))
                .username(str(claims.get("preferred_username")))
                .build();
    }

    private PublicKey key(String kid) {
        PublicKey k = kid != null ? keys.get(kid) : null;
        if (k != null) return k;
        if (Duration.between(keysFetchedAt, Instant.now()).compareTo(JWKS_MIN_REFRESH) > 0 || keys.isEmpty()) {
            loadKeys();
        }
        if (kid != null) {
            k = keys.get(kid);
        } else if (keys.size() == 1) {
            k = keys.values().iterator().next();
        }
        if (k == null) {
            throw new IllegalStateException("JWKS da kalit topilmadi: kid=" + kid);
        }
        return k;
    }

    private synchronized void loadKeys() {
        String raw = http.getForObject(JWKS_URL, String.class);
        Map<?, ?> jwks = parseJson(raw);
        if (jwks == null) {
            throw new IllegalStateException("JWKS javobini o'qib bo'lmadi: " + redact(raw));
        }
        Object list = jwks != null ? jwks.get("keys") : null;
        if (!(list instanceof Collection<?> items)) {
            throw new IllegalStateException("JWKS formati noto'g'ri");
        }
        keys.clear();
        for (Object o : items) {
            if (!(o instanceof Map<?, ?> jwk)) continue;
            if (!"RSA".equals(jwk.get("kty"))) continue;
            try {
                Base64.Decoder d = Base64.getUrlDecoder();
                RSAPublicKeySpec spec = new RSAPublicKeySpec(
                        new BigInteger(1, d.decode(String.valueOf(jwk.get("n")))),
                        new BigInteger(1, d.decode(String.valueOf(jwk.get("e")))));
                PublicKey pk = KeyFactory.getInstance("RSA").generatePublic(spec);
                keys.put(String.valueOf(jwk.get("kid")), pk);
            } catch (Exception e) {
                log.warn("JWKS kalitini o'qib bo'lmadi: {}", e.getMessage());
            }
        }
        keysFetchedAt = Instant.now();
    }

    private Map<?, ?> parseJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            Object v = mapper.readValue(raw.trim(), Object.class);
            return v instanceof Map<?, ?> m ? m : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String redact(String raw) {
        if (raw == null) return "null";
        String s = raw.replaceAll("(\"(?:id_token|access_token|refresh_token)\"\\s*:\\s*\")[^\"]*", "$1***");
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    private boolean audienceMatches(Object aud) {
        if (aud instanceof Number n) return clientId.equals(String.valueOf(n.longValue()));
        if (aud instanceof String s) return clientId.equals(s);
        if (aud instanceof Collection<?> c) return c.stream().anyMatch(a -> clientId.equals(String.valueOf(a)));
        return false;
    }

    private static String codeChallenge(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    @Getter
    @Builder
    public static class TelegramIdentity {
        private final Long id;
        private final String firstName;
        private final String lastName;
        private final String username;
    }
}
