package com.example.movieapp.dto.promo;

import com.example.movieapp.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PromoCodeAdminDto {
    private Long id;
    private String code;
    private DiscountType discountType;
    private Long discountValue;
    private Integer maxUses;
    private LocalDate validUntil;
    private boolean active;
    private LocalDateTime createdAt;
    private boolean allSeries;
    private List<SeriesRef> series;

    public record SeriesRef(Long id, String title) {
    }

    private long usedCount;         // muvaffaqiyatli to'lovlar soni
    private long paidUsers;         // to'lagan odamlar soni
    private long triedUsers;        // promokodni qo'llab to'lov ochgan odamlar (to'lamaganlar ham)
    private long totalDiscountInSom;
    private long totalRevenueInSom; // promokod bilan tushgan jami summa (komissiya bilan)
}
