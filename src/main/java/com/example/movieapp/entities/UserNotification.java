package com.example.movieapp.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String type;

    private String title;

    @Column(length = 1000)
    private String body;

    private String imageUrl;

    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
