package com.example.movieapp.service;

import com.example.movieapp.dto.payment.CreatePaymentOrderRequest;
import com.example.movieapp.dto.payment.CreatePaymentOrderResponse;
import com.example.movieapp.entities.Payment;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.PaymentProvider;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.enums.PaymentType;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepo userRepo;
    private final SeriesRepo seriesRepo;
    private final MovieAccessService movieAccessService;
    private final PixyService pixyService;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepo userRepo,
                          SeriesRepo seriesRepo,
                          MovieAccessService movieAccessService,
                          @Lazy PixyService pixyService) {
        this.paymentRepository = paymentRepository;
        this.userRepo = userRepo;
        this.seriesRepo = seriesRepo;
        this.movieAccessService = movieAccessService;
        this.pixyService = pixyService;
    }

    @Transactional
    public CreatePaymentOrderResponse createOrder(CreatePaymentOrderRequest request, Long userId) {
        if (request.getSeriesId() == null) {
            throw new RuntimeException("seriesId ko'rsatilishi kerak");
        }

        int durationMonths = resolveDuration(request.getDurationMonths());
        int accessDays = durationMonths * 30;

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User topilmadi: " + userId));

        Series series = seriesRepo.findById(request.getSeriesId())
                .orElseThrow(() -> new RuntimeException("Serial topilmadi: " + request.getSeriesId()));

        long baseAmountSom = resolvePrice(series, durationMonths);
        long commissionSom = baseAmountSom * 4 / 100;
        long amountSom = baseAmountSom + commissionSom;
        long amountTiyin = amountSom * 100;

        Payment payment = Payment.builder()
                .user(user)
                .series(series)
                .amount(amountTiyin)
                .status(PaymentStatus.PENDING)
                .provider(PaymentProvider.PIXY)
                .paymentType(PaymentType.INDIVIDUAL_SERIES)
                .subscriptionDays(accessDays)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment order yaratildi: id={}, user={}, series={}, davomiylik={}oy, amount={} tiyin",
                payment.getId(), user.getId(), series.getId(), durationMonths, amountTiyin);

        String payUrl = pixyService.createPayment(payment, amountSom);

        return CreatePaymentOrderResponse.builder()
                .orderId(payment.getId())
                .durationMonths(durationMonths)
                .accessDays(accessDays)
                .baseAmountInSom(baseAmountSom)
                .commissionInSom(commissionSom)
                .amountInSom(amountSom)
                .amount(amountTiyin)
                .provider(PaymentProvider.PIXY)
                .payUrl(payUrl)
                .build();
    }

    @Transactional
    public void activateAccess(Payment payment) {
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
    }

    private int resolveDuration(Integer durationMonths) {
        if (durationMonths == null || durationMonths == 1) return 1;
        if (durationMonths == 3) return 3;
        throw new RuntimeException("durationMonths faqat 1 yoki 3 bo'lishi mumkin");
    }

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

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }
}
