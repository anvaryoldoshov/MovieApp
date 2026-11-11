package com.example.movieapp.controller;

import com.example.movieapp.enums.Role;
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
import com.example.movieapp.service.TelegramLoginService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final TelegramLoginService telegramLoginService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        try {
            String deviceId = httpRequest.getHeader("X-Device-Id");

            refreshTokenService.validateRefreshToken(request.getRefreshToken());
            String email = refreshTokenService.getEmailFromToken(request.getRefreshToken());

            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User topilmadi"));

            Optional<UserDevice> userDeviceOpt = userDeviceRepository.findByUserId(user.getId());

            boolean admin = user.getRole() == Role.ADMIN;
            if (!admin && (userDeviceOpt.isEmpty() || !Objects.equals(userDeviceOpt.get().getDeviceId(), deviceId))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Ushbu qurilma ushbu akkaunt bilan bog'liq emas");
            }

            String newAccessToken = jwtTokenProvider.generateAccessToken(email, user.getRole());
            String newRefreshToken = refreshTokenService.createRefreshToken(email).getToken();

            if (!admin) {
                UserDevice device = userDeviceOpt.get();
                device.setToken(newAccessToken);
                device.setCreatedAt(Instant.now());
                userDeviceRepository.save(device);
            }

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

    @GetMapping("/telegram/start")
    public ResponseEntity<?> telegramStart(@RequestParam("deviceId") String deviceId) {
        return telegramLoginService.start(deviceId);
    }

    @GetMapping(value = "/telegram/callback", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> telegramCallback(@RequestParam(value = "state", required = false) String state,
                                                   @RequestParam(value = "code", required = false) String code,
                                                   @RequestParam(value = "error", required = false) String error) {
        TelegramLoginService.CallbackResult result = telegramLoginService.callback(state, code, error);
        String title;
        String text;
        switch (result) {
            case SUCCESS -> {
                title = "Muvaffaqiyatli kirdingiz";
                text = "Ilovaga qayting, kirish avtomatik davom etadi.";
            }
            case CANCELLED -> {
                title = "Kirish bekor qilindi";
                text = "Ilovaga qaytib, qaytadan urinib ko'ring.";
            }
            case EXPIRED -> {
                title = "Havola eskirgan";
                text = "Ilovaga qaytib, Telegram orqali qaytadan kiring.";
            }
            default -> {
                title = "Xatolik yuz berdi";
                text = "Ilovaga qaytib, qaytadan urinib ko'ring.";
            }
        }
        String html = "<!doctype html><html lang=\"uz\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>Tarixiy Kinolar</title>"
                + "<style>body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
                + "background:#0f1115;color:#fff;font-family:system-ui,sans-serif;text-align:center;padding:24px;box-sizing:border-box}"
                + "h1{font-size:22px;margin:0 0 12px}p{color:#aab;font-size:16px;margin:0}</style></head><body><div>"
                + "<h1>" + title + "</h1><p>" + text + "</p></div></body></html>";
        return ResponseEntity.status(result == TelegramLoginService.CallbackResult.SUCCESS ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .header("Cache-Control", "no-store")
                .body(html);
    }

    @GetMapping("/telegram/check")
    public ResponseEntity<?> telegramCheck(@RequestParam("code") String code,
                                           @RequestParam("deviceId") String deviceId) {
        return telegramLoginService.check(code, deviceId);
    }

}
