package com.example.movieapp.controller;

import com.example.movieapp.dto.CommentDto;
import com.example.movieapp.dto.CommentPageDto;
import com.example.movieapp.dto.CommentRequest;
import com.example.movieapp.entities.User;
import com.example.movieapp.exception.UserNotFoundException;
import com.example.movieapp.repository.UserRepo;
import com.example.movieapp.service.CommentService;
import com.example.movieapp.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepo userRepo;
    private final ReminderService reminderService;

    private User currentUser(Authentication authentication) {
        return userRepo.findByEmail(authentication.getName()).orElseThrow(UserNotFoundException::new);
    }

    @GetMapping("/series/{seriesId}/comments")
    public ResponseEntity<CommentPageDto> list(@PathVariable Long seriesId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               Authentication authentication) {
        return ResponseEntity.ok(commentService.list(seriesId, currentUser(authentication).getId(), page, size));
    }

    @PostMapping("/series/{seriesId}/comments")
    public ResponseEntity<CommentDto> add(@PathVariable Long seriesId,
                                          @RequestBody CommentRequest request,
                                          Authentication authentication) {
        return ResponseEntity.ok(commentService.add(seriesId, currentUser(authentication), request.getText()));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        commentService.delete(id, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/series/{seriesId}/remind")
    public ResponseEntity<Map<String, Boolean>> toggleReminder(@PathVariable Long seriesId, Authentication authentication) {
        return ResponseEntity.ok(Map.of("reminded", reminderService.toggle(seriesId, currentUser(authentication))));
    }

    @PostMapping("/comments/{id}/report")
    public ResponseEntity<Void> report(@PathVariable Long id, Authentication authentication) {
        commentService.report(id, currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/comments")
    public ResponseEntity<CommentPageDto> adminList(@RequestParam(required = false) Long seriesId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(commentService.adminList(seriesId, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/comments/{id}/hidden")
    public ResponseEntity<Void> setHidden(@PathVariable Long id, @RequestParam boolean value) {
        commentService.setHidden(id, value);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/comments/{id}")
    public ResponseEntity<Void> adminDelete(@PathVariable Long id) {
        commentService.adminDelete(id);
        return ResponseEntity.noContent().build();
    }
}
