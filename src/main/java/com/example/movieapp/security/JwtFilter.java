package com.example.movieapp.security;

import com.example.movieapp.entities.User;
import com.example.movieapp.entities.UserDevice;
import com.example.movieapp.repository.UserDeviceRepository;
import com.example.movieapp.repository.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepo userRepo;
    private final UserDeviceRepository userDeviceRepository;

    public JwtFilter(JwtTokenProvider jwtTokenProvider, UserRepo userRepo, UserDeviceRepository userDeviceRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepo = userRepo;
        this.userDeviceRepository = userDeviceRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String deviceId = request.getHeader("X-Device-Id");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    throw new AuthenticationServiceException("Token noto‘g‘ri yoki muddati tugagan");
                }

                String email = jwtTokenProvider.getEmailFromToken(token);
                User user = userRepo.findByEmail(email)
                        .orElseThrow(() -> new AuthenticationServiceException("Foydalanuvchi topilmadi"));

                UserDevice device = userDeviceRepository.findByUserAndDeviceId(user, deviceId)
                        .orElseThrow(() -> new AuthenticationServiceException("Ushbu foydalanuvchiga device biriktirilmagan"));

                if (!device.getToken().equals(token)) {
                    logger.error("Token mos emas ,{} ,{}",device.getToken(),token);
                    throw new AuthenticationServiceException("Token mos emas");
                }

                if (device.getDeviceId() == null || !device.getDeviceId().equals(deviceId)) {
                    logger.info(deviceId);
                    logger.error("Device ID mos emas yoki yo‘q, {}",  device.getDeviceId());
                    throw new AuthenticationServiceException("Device ID mos emas yoki yo‘q");
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (AuthenticationServiceException ex) {
                System.out.println(ex.getMessage());
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") || path.startsWith("/public/");
    }
}
