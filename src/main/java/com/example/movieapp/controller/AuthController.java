package com.example.movieapp.controller;

import com.example.movieapp.dto.AuthResponse;
import com.example.movieapp.dto.GoogleLoginRequest;
import com.example.movieapp.dto.LogoutRequest;
import com.example.movieapp.dto.RefreshRequest;
import com.example.movieapp.dto.SignInRequest;
import com.example.movieapp.entities.User;
import com.example.movieapp.entities.UserDevice;
import com.example.movieapp.repository.UserDeviceRepository;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.security.JwtTokenProvider;
import com.example.movieapp.service.AuthService;
import com.example.movieapp.service.RefreshTokenService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepo userRepo;
    private final UserDeviceRepository userDeviceRepository;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        try {
            String deviceId = httpRequest.getHeader("X-Device-Id");

            refreshTokenService.validateRefreshToken(request.getRefreshToken());
            String email = refreshTokenService.getEmailFromToken(request.getRefreshToken());

            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User topilmadi"));

            Optional<UserDevice> userDeviceOpt = userDeviceRepository.findByUserId(user.getId());

            if (userDeviceOpt.isEmpty() || !Objects.equals(userDeviceOpt.get().getDeviceId(), deviceId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Ushbu qurilma ushbu akkaunt bilan bog'liq emas");
            }

            String newAccessToken = jwtTokenProvider.generateAccessToken(email, user.getRole());
            String newRefreshToken = refreshTokenService.createRefreshToken(email).getToken();

            UserDevice device = userDeviceOpt.get();
            device.setToken(newAccessToken);
            device.setCreatedAt(Instant.now());
            userDeviceRepository.save(device);

            AuthResponse response = new AuthResponse();
            response.setToken(newAccessToken);
            response.setRefreshToken(newRefreshToken);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.getEmail());
        return ResponseEntity.ok("User logged out successfully");
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> signIn(@RequestBody SignInRequest request) {
        return authService.signIn(request);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest request) {
        GoogleIdToken idToken = authService.verifyGoogleToken(request.getCredential());
        return authService.getAuthResponseResponseEntity(idToken, request.getDeviceId());
    }

}
