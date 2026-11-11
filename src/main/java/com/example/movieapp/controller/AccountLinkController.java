package com.example.movieapp.controller;

import com.example.movieapp.entities.User;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.GoogleLinkService;
import com.example.movieapp.service.TelegramLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AccountLinkController {

    private final TelegramLoginService telegramLoginService;
    private final GoogleLinkService googleLinkService;
    private final UserRepo userRepo;

    @PostMapping("/account/telegram/link/start")
    public ResponseEntity<?> startTelegramLink() {
        return telegramLoginService.startLink(currentUser());
    }

    @GetMapping("/account/telegram/link/check")
    public ResponseEntity<?> checkTelegramLink(@RequestParam("code") String code) {
        return telegramLoginService.checkLink(code, currentUser());
    }

    @PostMapping("/account/telegram/sync")
    public ResponseEntity<?> syncTelegram() {
        return telegramLoginService.syncSubscriptions(currentUser());
    }

    @PostMapping("/account/google/link")
    public ResponseEntity<?> linkGoogle(@RequestBody Map<String, String> body) {
        return googleLinkService.link(currentUser(), body.get("credential"), body.get("deviceId"));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email).orElseThrow(UserNotFoundException::new);
    }
}
