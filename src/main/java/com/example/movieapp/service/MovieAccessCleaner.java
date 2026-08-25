package com.example.movieapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieAccessCleaner {

    private final MovieAccessService movieAccessService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredAccesses() {
        movieAccessService.removeExpiredAccesses();
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void sendExpiryReminders() {
        movieAccessService.sendExpiryReminders();
    }
}