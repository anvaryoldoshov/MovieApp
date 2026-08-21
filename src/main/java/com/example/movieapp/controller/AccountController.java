package com.example.movieapp.controller;

import com.example.movieapp.dto.BaseMessage;
import com.example.movieapp.dto.DeleteAccountConfirmRequest;
import com.example.movieapp.dto.DeleteAccountRequest;
import com.example.movieapp.dto.FcmTokenRequest;
import com.example.movieapp.exception.InvalidDeleteCodeException;
import com.example.movieapp.service.AccountDeletionCodeService;
import com.example.movieapp.service.EmailService;
import com.example.movieapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final AccountDeletionCodeService deletionCodeService;
    private final EmailService emailService;

    // Ilova ichidan, JWT bilan autentifikatsiya qilingan foydalanuvchi o'z hisobini o'chiradi.
    @DeleteMapping("/account/me")
    public ResponseEntity<BaseMessage> deleteMyAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.deleteAccountByEmail(email);
        return ResponseEntity.ok(new BaseMessage(200, "Hisobingiz va unga bog'liq barcha ma'lumotlar muvaffaqiyatli o'chirildi"));
    }

    // Mobil ilova login qilingandan keyin FCM tokenini shu yerga yuboradi (push-notification uchun).
    @PutMapping("/account/fcm-token")
    public ResponseEntity<BaseMessage> updateFcmToken(@RequestBody FcmTokenRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.updateFcmToken(email, request.getFcmToken());
        return ResponseEntity.ok(new BaseMessage(200, "FCM token saqlandi"));
    }

    // 1-qadam: ilovasi bo'lmagan foydalanuvchi email kiritadi, emailga tasdiqlash kodi yuboriladi.
    @PostMapping("/public/account/delete-request")
    public ResponseEntity<BaseMessage> requestAccountDeletion(@RequestBody DeleteAccountRequest request) {
        userService.assertUserExistsByEmail(request.getEmail());
        String code = deletionCodeService.generateCode(request.getEmail());
        emailService.sendAccountDeletionCode(request.getEmail(), code);
        return ResponseEntity.ok(new BaseMessage(200, "Tasdiqlash kodi emailingizga yuborildi"));
    }

    // 2-qadam: foydalanuvchi emailga kelgan kodni kiritadi, kod to'g'ri bo'lsa hisob o'chiriladi.
    @PostMapping("/public/account/delete-confirm")
    public ResponseEntity<BaseMessage> confirmAccountDeletion(@RequestBody DeleteAccountConfirmRequest request) {
        if (!deletionCodeService.verifyCode(request.getEmail(), request.getCode())) {
            throw new InvalidDeleteCodeException();
        }
        userService.deleteAccountByEmail(request.getEmail());
        return ResponseEntity.ok(new BaseMessage(200, "Hisobingiz va unga bog'liq barcha ma'lumotlar muvaffaqiyatli o'chirildi"));
    }
}
