package com.example.movieapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SeriesDto {

    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String status;

    private String imagePath;

    private boolean hasAccess;

    private boolean hasEpisode;

    private Long monthlyPrice;   // null bo'lsa 1 oylik tarif yo'q
    private Long quarterlyPrice; // null bo'lsa 3 oylik tarif yo'q

    private List<Long> genreIds;   // janrlarni biriktirish uchun (create/update)
    private List<GenreDto> genres; // javobda ko'rsatish uchun

    // Platformada har serialning faqat birinchi qismi bepul (FreeEpisodes). Qo'shimcha bepul
    // qismlar Telegram kanalda bo'lishi mumkin — ilova shu havolani va qismlar sonini ko'rsatadi.
    private String telegramFreeUrl;   // bo'sh = Telegram'da bepul qism yo'q
    private Integer telegramFreeCount; // Telegram'dagi bepul qismlar soni (ko'rsatish uchun)

    private String bunnyCollectionId; // Bunny Stream Collection ID yoki URL (server tomonda tozalanadi)

    /**
     * true  Ã¢â€ â€™ obuna seriali (SubscriptionPlan orqali ko'riladi, alohida narxi yo'q)
     * false Ã¢â€ â€™ alohida sotib olinadigan serial (monthlyPrice/quarterlyPrice kerak)
     */
    private Boolean subscriptionBased;

    private Long viewCount; // serial detali necha marta ochilgani (faqat o'qish)

}

