package com.example.movieapp.controller;

import com.example.movieapp.dto.UserNotificationDto;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Foydalanuvchining shaxsiy notifikatsiyalar oynasi")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;
    private final UserRepo userRepo;

    @GetMapping("/me")
    @Operation(summary = "Oxirgi 30 kunlik notifikatsiyalar ro'yxati", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<UserNotificationDto>> getMyNotifications(
            @RequestParam(value = "days", defaultValue = "30") int days,
            Authentication authentication
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(userNotificationService.getRecent(user.getId(), days));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "O'qilmagan notifikatsiyalar soni (badge uchun)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(Map.of("count", userNotificationService.getUnreadCount(user.getId())));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Bitta notifikatsiyani o'qilgan deb belgilash", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        userNotificationService.markAsRead(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Barcha notifikatsiyalarni o'qilgan qilish", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        User user = currentUser(authentication);
        userNotificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }

    private User currentUser(Authentication authentication) {
        return userRepo.findByEmail(authentication.getName())
                .orElseThrow(UserNotFoundException::new);
    }
}
