package com.example.movieapp.controller;

import com.example.movieapp.dto.BaseMessage;
import com.example.movieapp.dto.DeleteAccountRequest;
import com.example.movieapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    // Ilova ichidan, JWT bilan autentifikatsiya qilingan foydalanuvchi o'z hisobini o'chiradi.
    @DeleteMapping("/account/me")
    public ResponseEntity<BaseMessage> deleteMyAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.deleteAccountByEmail(email);
        return ResponseEntity.ok(new BaseMessage(200, "Hisobingiz va unga bog'liq barcha ma'lumotlar muvaffaqiyatli o'chirildi"));
    }

    // Ilovasi bo'lmagan yoki kira olmayotgan foydalanuvchilar uchun veb-forma orqali o'chirish so'rovi.
    @PostMapping("/public/account/delete-request")
    public ResponseEntity<BaseMessage> requestAccountDeletion(@RequestBody DeleteAccountRequest request) {
        userService.deleteAccountByEmailAndPassword(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new BaseMessage(200, "Hisobingiz va unga bog'liq barcha ma'lumotlar muvaffaqiyatli o'chirildi"));
    }
}
