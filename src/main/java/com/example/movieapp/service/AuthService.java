package com.example.movieapp.service;

import com.example.movieapp.dto.AuthResponse;
import com.example.movieapp.dto.SignInRequest;
import com.example.movieapp.dto.SignUpRequest;
import com.example.movieapp.entities.RefreshToken;
import com.example.movieapp.entities.User;
import com.example.movieapp.entities.UserDevice;
import com.example.movieapp.mapper.UserMapper;
import com.example.movieapp.repository.RefreshTokenRepository;
import com.example.movieapp.repository.UserDeviceRepository;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final JwtTokenProvider jwtService;

    @Value("${mobile.google.client.Id}")
    private String mobileGoogleId;

    @Value("${web.google.client.Id}")
    private String webGoogleId;

    public AuthResponse signIn(SignInRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow( () ->  {
                    log.error("User not Found");
                    return new RuntimeException("User not found");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.debug("Invalid Password");
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        String refreshToken;
        if (existingToken.isPresent() && existingToken.get().getExpiryDate().isAfter(Instant.now())) {
            refreshToken = existingToken.get().getToken();
        } else {
            refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();
        }

        userDeviceCreateOrUpdate(request.getDeviceId(), user, accessToken);

        AuthResponse response = userMapper.toAuthResponse(user);
        response.setDeviceId(request.getDeviceId());
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    private void userDeviceCreateOrUpdate(String deviceId, User user, String accessToken) {
        Optional<UserDevice> existingDevice = userDeviceRepository.findByUserId(user.getId());

        if (existingDevice.isPresent()) {
            UserDevice device = existingDevice.get();
            device.setToken(accessToken);
            device.setDeviceId(deviceId);
            device.setCreatedAt(Instant.now());
            userDeviceRepository.save(device);
        }else {
            UserDevice newDevice = new UserDevice();
            newDevice.setUser(user);
            newDevice.setToken(accessToken);
            newDevice.setDeviceId(deviceId);
            newDevice.setCreatedAt(Instant.now());
            userDeviceRepository.save(newDevice);
        }
    }


    public AuthResponse signUp(SignUpRequest request) {
        if (userRepo.findByEmail(request.getEmail()).isPresent())
            throw new RuntimeException("User already exists");

        Long userId = generateUniqueUserId();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .subscription(false)
                .userId(userId)
                .build();

        userRepo.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();

        AuthResponse response = userMapper.toAuthResponse(user);
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    public void logout(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        refreshTokenService.deleteByUser(user);
    }

    public Long generateUniqueUserId() {
        Random random = new Random();
        Long userId;
        do {
            int number = 100000 + random.nextInt(900000);
            userId = (long) number;
        } while (userRepo.existsByUserId(userId));
        return userId;
    }

    public ResponseEntity<AuthResponse> getAuthResponseResponseEntity(GoogleIdToken idToken, String deviceId) {
        if (idToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepo.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .username(name)
                            .subscription(false)
                            .userId(generateUniqueUserId())
                            .build();
                    return userRepo.save(newUser);
                });

        String accessToken = jwtService.generateAccessToken(email);
        String refreshToken = refreshTokenService.createRefreshToken(email).getToken();


        userDeviceCreateOrUpdate(deviceId, user, accessToken);

        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setSubscription(user.getSubscription());
        response.setUserId(user.getUserId());
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setDeviceId(deviceId);

        return ResponseEntity.ok(response);
    }


    public GoogleIdToken verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Arrays.asList(mobileGoogleId, webGoogleId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.error("Google Id token verification failed");
                return null;
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            log.info(payload.getSubject());
            return idToken;

        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

}

