package com.example.movieapp.entities;

import com.example.movieapp.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promo_codes")
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Har doim katta harflarda saqlanadi (masalan: KINO15)
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    // PERCENT bo'lsa foiz (1-100), AMOUNT bo'lsa so'm
    @Column(nullable = false)
    private Long discountValue;

    // Jami necha marta ishlatish mumkin, null = cheksiz
    private Integer maxUses;

    // Shu sanagacha (shu kun ham) amal qiladi, null = muddatsiz
    private LocalDate validUntil;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Qaysi seriallarga amal qiladi; bo'sh bo'lsa — barcha seriallar va obuna uchun
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "promo_code_series",
            joinColumns = @JoinColumn(name = "promo_code_id"),
            inverseJoinColumns = @JoinColumn(name = "series_id"))
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Series> series = new HashSet<>();

    public boolean appliesToAll() {
        return series == null || series.isEmpty();
    }

    public boolean appliesTo(Long seriesId) {
        return appliesToAll() || (seriesId != null && series.stream().anyMatch(s -> s.getId().equals(seriesId)));
    }

    private String createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
