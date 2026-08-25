package com.example.movieapp.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class MovieAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Series movie;

    @ManyToOne
    private User user;

    private boolean paid;

    @CreationTimestamp
    private LocalDateTime createdAt;


    private LocalDate accessEndDate;

    private boolean reminderSent;
}
