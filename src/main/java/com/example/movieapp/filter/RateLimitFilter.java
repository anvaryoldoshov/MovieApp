package com.example.movieapp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Har bir IP uchun: [so'rovlar soni, oyna boshlanish vaqti]
    private final ConcurrentHashMap<String, long[]> ipCounters = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/payment");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        long[] counter = ipCounters.compute(ip, (k, v) -> {
            if (v == null || now - v[1] > WINDOW_MS) {
                return new long[]{1, now};
            }
            v[0]++;
            return v;
        });

        if (counter[0] > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit: IP={} bloklandi, {} so'rov/daqiqa", ip, counter[0]);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Juda ko'p so'rov. 1 daqiqadan so'ng qayta urinib ko'ring.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
