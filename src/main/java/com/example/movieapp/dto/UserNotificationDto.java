package com.example.movieapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationDto {
    private Long id;
    private String type;
    private String title;
    private String body;
    private String imageUrl;
    private boolean read;
    private LocalDateTime createdAt;
}
