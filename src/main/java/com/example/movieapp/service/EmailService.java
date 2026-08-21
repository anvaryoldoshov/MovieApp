package com.example.movieapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    public void sendAccountDeletionCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Hisobni o'chirish - tasdiqlash kodi");
        message.setText("Hisobingizni o'chirish uchun tasdiqlash kodi: " + code +
                "\n\nBu kod 10 daqiqa davomida amal qiladi." +
                "\nAgar siz bu so'rovni yubormagan bo'lsangiz, bu xabarni e'tiborsiz qoldiring.");
        mailSender.send(message);
    }
}
