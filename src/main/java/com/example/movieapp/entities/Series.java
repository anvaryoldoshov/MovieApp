package com.example.movieapp.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "series")
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imagePath;

    @Column(columnDefinition = "TEXT")
    private String title;

    private String status;

    private Long monthlyPrice;   // 1 oylik narx (null = tarif yo'q)
    private Long quarterlyPrice; // 3 oylik narx (null = tarif yo'q)

    private Integer sortOrder; // adminda ro'yxatdagi tartibi (kichik = birinchi)

    // Platformada har serialning faqat birinchi qismi bepul (FreeEpisodes). Qo'shimcha bepul
    // qismlar Telegram kanalda bo'lishi mumkin — ilova shu havolani va qismlar sonini ko'rsatadi.
    private String telegramFreeUrl;   // bo'sh = Telegram'da bepul qism yo'q
    private Integer telegramFreeCount; // Telegram'dagi bepul qismlar soni (ko'rsatish uchun)

    private String bunnyCollectionId; // Bunny Stream'dagi shu serialga tegishli Collection GUID'i (ixtiyoriy)

    // Serial detali necha marta ochilgani. Bazada ustun NOT NULL, shuning uchun yangi
    // serial 0 bilan saqlanishi shart (aks holda "null value in column view_count" xatosi).
    @Column(nullable = false)
    private Long viewCount = 0L;

    /**
     * true  Ã¢â€ â€™ bu serial faqat obuna (SubscriptionPlan) orqali ko'riladi;
     *          monthlyPrice/quarterlyPrice ishlatilmaydi.
     * false Ã¢â€ â€™ bu serial alohida sotib olinadi (monthlyPrice/quarterlyPrice kerak).
     */
    private Boolean subscriptionBased;

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Banner> banners;

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Episode> episodes;

    @ManyToMany
    @JoinTable(
            name = "series_genres",
            joinColumns = @JoinColumn(name = "series_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;

}


