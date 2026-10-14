package com.example.movieapp.service;

import com.example.movieapp.dto.promo.PromoCodeAdminDto;
import com.example.movieapp.dto.promo.PromoCodeRequest;
import com.example.movieapp.dto.promo.PromoUsageDto;
import com.example.movieapp.entities.Payment;
import com.example.movieapp.entities.PromoCode;
import com.example.movieapp.entities.Series;
import com.example.movieapp.enums.DiscountType;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.exception.ErrorCode;
import com.example.movieapp.exception.PromoCodeException;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.PromoCodeRepository;
import com.example.movieapp.repository.SeriesRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    /** Chegirmadan keyin narx bundan pastga tushmaydi (to'lov tizimi juda kichik summani qabul qilmaydi) */
    public static final long MIN_PRICE_SOM = 1000;

    private final PromoCodeRepository promoCodeRepository;
    private final PaymentRepository paymentRepository;
    private final SeriesRepo seriesRepo;

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase().replaceAll("\\s+", "");
    }

    /**
     * Foydalanuvchi kiritgan promokodni tekshiradi; yaroqsiz bo'lsa sababi bilan xato beradi.
     * seriesId — individual serial xaridi uchun, obuna xaridida null (obunaga faqat "barcha seriallar" promokodi amal qiladi).
     */
    public PromoCode validateFor(String rawCode, Long userId, Long seriesId) {
        String code = normalize(rawCode);
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new PromoCodeException(ErrorCode.PROMO_NOT_FOUND));
        if (!promo.isActive()) throw new PromoCodeException(ErrorCode.PROMO_INACTIVE);
        if (!promo.appliesTo(seriesId)) throw new PromoCodeException(ErrorCode.PROMO_NOT_APPLICABLE);
        if (promo.getValidUntil() != null && LocalDate.now().isAfter(promo.getValidUntil())) {
            throw new PromoCodeException(ErrorCode.PROMO_EXPIRED);
        }
        if (promo.getMaxUses() != null
                && paymentRepository.countByPromoCode_IdAndStatus(promo.getId(), PaymentStatus.PAID) >= promo.getMaxUses()) {
            throw new PromoCodeException(ErrorCode.PROMO_LIMIT_REACHED);
        }
        if (paymentRepository.existsByUser_IdAndPromoCode_IdAndStatus(userId, promo.getId(), PaymentStatus.PAID)) {
            throw new PromoCodeException(ErrorCode.PROMO_ALREADY_USED);
        }
        return promo;
    }

    /** Komissiyasiz narxdan chegirma summasi (so'mda) */
    public long discountFor(PromoCode promo, long baseSom) {
        long discount = promo.getDiscountType() == DiscountType.PERCENT
                ? Math.round(baseSom * promo.getDiscountValue() / 100.0)
                : promo.getDiscountValue();
        long maxDiscount = Math.max(0, baseSom - MIN_PRICE_SOM);
        return Math.max(0, Math.min(discount, maxDiscount));
    }

    // ── Admin ────────────────────────────────────────────────────────────

    public List<PromoCodeAdminDto> findAll() {
        Map<Long, Object[]> stats = new HashMap<>();
        for (Object[] row : paymentRepository.promoCodeStats()) {
            stats.put((Long) row[0], row);
        }
        return promoCodeRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(p -> toDto(p, stats.get(p.getId())))
                .toList();
    }

    @Transactional
    public PromoCodeAdminDto create(PromoCodeRequest request, String adminEmail) {
        String code = validateRequest(request);
        if (promoCodeRepository.existsByCodeIgnoreCase(code)) {
            throw new PromoCodeException(ErrorCode.PROMO_ALREADY_EXISTS);
        }
        PromoCode promo = PromoCode.builder()
                .code(code)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxUses(positiveOrNull(request.getMaxUses()))
                .validUntil(request.getValidUntil())
                .active(request.getActive() == null || request.getActive())
                .createdBy(adminEmail)
                .series(resolveSeries(request.getSeriesIds()))
                .build();
        return toDto(promoCodeRepository.save(promo), null);
    }

    @Transactional
    public PromoCodeAdminDto update(Long id, PromoCodeRequest request) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new PromoCodeException(ErrorCode.PROMO_NOT_FOUND));
        String code = validateRequest(request);
        if (promoCodeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new PromoCodeException(ErrorCode.PROMO_ALREADY_EXISTS);
        }
        promo.setCode(code);
        promo.setDiscountType(request.getDiscountType());
        promo.setDiscountValue(request.getDiscountValue());
        promo.setMaxUses(positiveOrNull(request.getMaxUses()));
        promo.setValidUntil(request.getValidUntil());
        if (request.getActive() != null) promo.setActive(request.getActive());
        promo.setSeries(resolveSeries(request.getSeriesIds()));
        promoCodeRepository.save(promo);
        return findAll().stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    public void delete(Long id) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new PromoCodeException(ErrorCode.PROMO_NOT_FOUND));
        if (paymentRepository.countByPromoCode_Id(id) > 0) {
            throw new PromoCodeException(ErrorCode.PROMO_IN_USE);
        }
        promoCodeRepository.delete(promo);
    }

    public List<PromoUsageDto> usages(Long id) {
        if (!promoCodeRepository.existsById(id)) throw new PromoCodeException(ErrorCode.PROMO_NOT_FOUND);
        return paymentRepository.findByPromoCode_IdOrderByCreatedAtDesc(id).stream()
                .map(this::toUsage)
                .toList();
    }

    private String validateRequest(PromoCodeRequest r) {
        String code = normalize(r.getCode());
        if (!code.matches("[A-Z0-9_-]{3,40}") || r.getDiscountType() == null
                || r.getDiscountValue() == null || r.getDiscountValue() <= 0
                || (r.getDiscountType() == DiscountType.PERCENT && r.getDiscountValue() > 100)) {
            throw new PromoCodeException(ErrorCode.PROMO_INVALID);
        }
        return code;
    }

    private Set<Series> resolveSeries(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(seriesRepo.findAllById(ids));
    }

    private static Integer positiveOrNull(Integer v) {
        return v == null || v <= 0 ? null : v;
    }

    private PromoCodeAdminDto toDto(PromoCode p, Object[] s) {
        return PromoCodeAdminDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maxUses(p.getMaxUses())
                .validUntil(p.getValidUntil())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .allSeries(p.appliesToAll())
                .series(p.getSeries().stream()
                        .sorted(Comparator.comparing(Series::getId))
                        .map(sr -> new PromoCodeAdminDto.SeriesRef(sr.getId(), sr.getTitle()))
                        .toList())
                .usedCount(s == null ? 0 : ((Number) s[1]).longValue())
                .paidUsers(s == null ? 0 : ((Number) s[2]).longValue())
                .totalDiscountInSom(s == null ? 0 : ((Number) s[3]).longValue())
                .totalRevenueInSom(s == null ? 0 : ((Number) s[4]).longValue() / 100)
                .triedUsers(s == null ? 0 : ((Number) s[5]).longValue())
                .build();
    }

    private PromoUsageDto toUsage(Payment p) {
        String target = p.getSeries() != null
                ? p.getSeries().getTitle()
                : (p.getSubscriptionPlan() != null ? "Obuna: " + p.getSubscriptionPlan().getName() : "—");
        return PromoUsageDto.builder()
                .paymentId(p.getId())
                .userId(p.getUser().getId())
                .username(p.getUser().getUsername())
                .email(p.getUser().getEmail())
                .target(target)
                .days(p.getSubscriptionDays())
                .amountInSom(p.getAmount() / 100)
                .discountInSom(p.getDiscountAmount() == null ? 0 : p.getDiscountAmount())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
