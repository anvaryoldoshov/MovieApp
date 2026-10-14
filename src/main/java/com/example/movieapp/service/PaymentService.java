package com.example.movieapp.service;

import com.example.movieapp.dto.payment.CreatePaymentOrderRequest;
import com.example.movieapp.dto.payment.CreatePaymentOrderResponse;
import com.example.movieapp.entities.Payment;
import com.example.movieapp.entities.PromoCode;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.SubscriptionPlan;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.PaymentProvider;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.enums.PaymentType;
import com.example.movieapp.exception.SubscriptionPlanNotFoundException;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.SubscriptionPlanRepo;
import com.example.movieapp.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepo userRepo;
    private final SeriesRepo seriesRepo;
    private final SubscriptionPlanRepo subscriptionPlanRepo;
    private final MovieAccessService movieAccessService;
    private final PixyService pixyService;
    private final UserNotificationService userNotificationService;
    private final PromoCodeService promoCodeService;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepo userRepo,
                          SeriesRepo seriesRepo,
                          SubscriptionPlanRepo subscriptionPlanRepo,
                          MovieAccessService movieAccessService,
                          @Lazy PixyService pixyService,
                          UserNotificationService userNotificationService,
                          PromoCodeService promoCodeService) {
        this.paymentRepository = paymentRepository;
        this.userRepo = userRepo;
        this.seriesRepo = seriesRepo;
        this.subscriptionPlanRepo = subscriptionPlanRepo;
        this.movieAccessService = movieAccessService;
        this.pixyService = pixyService;
        this.userNotificationService = userNotificationService;
        this.promoCodeService = promoCodeService;
    }

    @Transactional
    public CreatePaymentOrderResponse createOrder(CreatePaymentOrderRequest request, Long userId) {
        Pricing pricing = price(request, userId);

        Payment payment = Payment.builder()
                .user(pricing.user())
                .series(pricing.series())
                .subscriptionPlan(pricing.plan())
                .amount(pricing.amountSom() * 100)
                .status(PaymentStatus.PENDING)
                .provider(PaymentProvider.PIXY)
                .paymentType(pricing.plan() != null ? PaymentType.SUBSCRIPTION : PaymentType.INDIVIDUAL_SERIES)
                .subscriptionDays(pricing.durationMonths() * 30)
                .promoCode(pricing.promo())
                .discountAmount(pricing.promo() != null ? pricing.discountSom() : null)
                .build();

        payment = paymentRepository.save(payment);
        log.info("{} to'lov order yaratildi: id={}, user={}, {}={}, davomiylik={}oy, amount={} tiyin{}",
                pricing.plan() != null ? "Obuna" : "Individual",
                payment.getId(), pricing.user().getId(),
                pricing.plan() != null ? "plan" : "series",
                pricing.plan() != null ? pricing.plan().getId() : pricing.series().getId(),
                pricing.durationMonths(), payment.getAmount(),
                pricing.promo() != null ? ", promo=" + pricing.promo().getCode() + " (-" + pricing.discountSom() + " so'm)" : "");

        String payUrl = pixyService.createPayment(payment, pricing.amountSom());

        return toResponse(pricing, payment.getId(), payUrl);
    }

    /** Order yaratmasdan narxni (promokod chegirmasi bilan) hisoblab beradi */
    @Transactional(readOnly = true)
    public CreatePaymentOrderResponse quote(CreatePaymentOrderRequest request, Long userId) {
        return toResponse(price(request, userId), null, null);
    }

    private record Pricing(User user, Series series, SubscriptionPlan plan, int durationMonths,
                           long originalSom, PromoCode promo, long discountSom,
                           long baseSom, long commissionSom, long amountSom) {
    }

    private Pricing price(CreatePaymentOrderRequest request, Long userId) {
        // Kamida bittasi ko'rsatilishi shart
        if (request.getSeriesId() == null && request.getSubscriptionPlanId() == null) {
            throw new RuntimeException("seriesId yoki subscriptionPlanId ko'rsatilishi kerak");
        }

        int durationMonths = resolveDuration(request.getDurationMonths());

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User topilmadi: " + userId));

        Series series = null;
        SubscriptionPlan plan = null;
        long originalSom;
        if (request.getSubscriptionPlanId() != null) {
            plan = subscriptionPlanRepo.findById(request.getSubscriptionPlanId())
                    .orElseThrow(SubscriptionPlanNotFoundException::new);
            originalSom = resolveSubscriptionPrice(plan, durationMonths);
        } else {
            series = seriesRepo.findById(request.getSeriesId())
                    .orElseThrow(() -> new RuntimeException("Serial topilmadi: " + request.getSeriesId()));
            originalSom = resolvePrice(series, durationMonths);
        }

        PromoCode promo = null;
        long discountSom = 0;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            promo = promoCodeService.validateFor(request.getPromoCode(), userId);
            discountSom = promoCodeService.discountFor(promo, originalSom);
        }

        long baseSom = originalSom - discountSom;
        long commissionSom = baseSom * 4 / 100;
        return new Pricing(user, series, plan, durationMonths, originalSom, promo, discountSom,
                baseSom, commissionSom, baseSom + commissionSom);
    }

    private CreatePaymentOrderResponse toResponse(Pricing p, Long orderId, String payUrl) {
        return CreatePaymentOrderResponse.builder()
                .orderId(orderId)
                .durationMonths(p.durationMonths())
                .accessDays(p.durationMonths() * 30)
                .originalAmountInSom(p.originalSom())
                .discountInSom(p.discountSom())
                .promoCode(p.promo() != null ? p.promo().getCode() : null)
                .baseAmountInSom(p.baseSom())
                .commissionInSom(p.commissionSom())
                .amountInSom(p.amountSom())
                .amount(p.amountSom() * 100)
                .provider(PaymentProvider.PIXY)
                .payUrl(payUrl)
                .build();
    }

    @Transactional
    public void activateAccess(Payment payment) {
        if (payment.getPaymentType() == PaymentType.SUBSCRIPTION) {
            // ── OBUNA AKTIVATSIYASI ─────────────────────────────────────
            // MovieAccess yozmaymiz: canUserWatchMovie() user.subscription + muddatini tekshiradi.
            User user = payment.getUser();
            LocalDate today = LocalDate.now();

            // Agar obuna hali muddati tugamagan bo'lsa — qolgan kunlar ustiga qo'shiladi
            LocalDate newEndDate;
            if (user.getSubscriptionEndDate() != null && !today.isAfter(user.getSubscriptionEndDate())) {
                newEndDate = user.getSubscriptionEndDate().plusDays(payment.getSubscriptionDays());
            } else {
                newEndDate = today.plusDays(payment.getSubscriptionDays());
            }

            user.setSubscription(true);
            user.setSubscriptionStartDate(today);
            user.setSubscriptionEndDate(newEndDate);
            userRepo.save(user);

            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);
            log.info("Obuna faollashtirildi: paymentId={}, userId={}, tugash={}",
                    payment.getId(), user.getId(), newEndDate);

            String planName = payment.getSubscriptionPlan() != null
                    ? payment.getSubscriptionPlan().getName()
                    : "Obuna";

            userNotificationService.notifyUser(
                    user,
                    "SUBSCRIPTION_ACTIVATED",
                    planName + " muvaffaqiyatli faollashtirildi!",
                    String.format("%d kunlik obuna faollashtirildi. Tugash sanasi: %s",
                            payment.getSubscriptionDays(), newEndDate),
                    null
            );

        } else {
            // ── INDIVIDUAL SERIAL AKTIVATSIYASI ─────────────────────────
            log.info("Access faollashtirilmoqda: paymentId={}, userId={}, seriesId={}, kunlar={}",
                    payment.getId(), payment.getUser().getId(), payment.getSeries().getId(), payment.getSubscriptionDays());

            movieAccessService.grantPaidAccess(
                    payment.getUser().getId(),
                    payment.getSeries().getId(),
                    payment.getSubscriptionDays()
            );

            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);
            log.info("Access faollashtirildi: paymentId={}", payment.getId());

            userNotificationService.notifyUser(
                    payment.getUser(),
                    "PURCHASE_SUCCESS",
                    "To'lov muvaffaqiyatli",
                    String.format("\"%s\" seriali uchun %d kunlik kirish faollashtirildi",
                            payment.getSeries().getTitle(), payment.getSubscriptionDays()),
                    null
            );
        }
    }

    // ── Yordamchi metodlar ─────────────────────────────────────────────────

    private int resolveDuration(Integer durationMonths) {
        if (durationMonths == null || durationMonths == 1) return 1;
        if (durationMonths == 3) return 3;
        throw new RuntimeException("durationMonths faqat 1 yoki 3 bo'lishi mumkin");
    }

    /** Individual serial narxini aniqlash */
    private long resolvePrice(Series series, int durationMonths) {
        if (durationMonths == 1) {
            if (series.getMonthlyPrice() == null)
                throw new RuntimeException("Bu serialda 1 oylik tarif mavjud emas");
            return series.getMonthlyPrice();
        } else {
            if (series.getQuarterlyPrice() == null)
                throw new RuntimeException("Bu serialda 3 oylik tarif mavjud emas");
            return series.getQuarterlyPrice();
        }
    }

    /** Obuna tarifi narxini aniqlash */
    private long resolveSubscriptionPrice(SubscriptionPlan plan, int durationMonths) {
        if (durationMonths == 1) {
            if (plan.getMonthlyPrice() == null)
                throw new RuntimeException("Bu obuna tarifida 1 oylik narx mavjud emas");
            return plan.getMonthlyPrice();
        } else {
            if (plan.getQuarterlyPrice() == null)
                throw new RuntimeException("Bu obuna tarifida 3 oylik narx mavjud emas");
            return plan.getQuarterlyPrice();
        }
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }
}
