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

import java.util.Collections;
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

    @Value("${payment.subscription.price.monthly:50000}")
    private long monthlyPriceSom;

    @Value("${payment.subscription.price.quarterly:130000}")
    private long quarterlyPriceSom;

    @Value("${payment.subscription.price.yearly:500000}")
    private long yearlyPriceSom;

    @Value("${payment.individual.series.price:10000}")
    private long individualPriceSom;

    @Value("${payment.individual.series.access.days:30}")
    private int individualAccessDays;

    @Transactional
    public CreatePaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        User user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User topilmadi: " + request.getUserId()));

        long amountSom;
        PaymentType type;
        Series series = null;
        Integer subscriptionDays = null;

        if (request.getSeriesId() != null) {
            series = seriesRepo.findById(request.getSeriesId())
                    .orElseThrow(() -> new RuntimeException("Series topilmadi: " + request.getSeriesId()));
            amountSom = individualPriceSom;
            type = PaymentType.INDIVIDUAL_SERIES;
        } else if (request.getSubscriptionDays() != null) {
            subscriptionDays = request.getSubscriptionDays();
            amountSom = calculateSubscriptionPrice(subscriptionDays);
            type = PaymentType.SUBSCRIPTION;
        } else {
            throw new RuntimeException("subscriptionDays yoki seriesId ko'rsatilishi kerak");
        }

        long amountTiyin = amountSom * 100;

        Payment payment = Payment.builder()
                .user(user)
                .series(series)
                .amount(amountTiyin)
                .status(PaymentStatus.PENDING)
                .provider(PaymentProvider.PIXY)
                .paymentType(type)
                .subscriptionDays(subscriptionDays)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment order yaratildi: id={}, user={}, amount={} tiyin",
                payment.getId(), user.getId(), amountTiyin);

        String payUrl = pixyService.createPayment(payment, amountSom);

        return CreatePaymentOrderResponse.builder()
                .orderId(payment.getId())
                .amount(amountTiyin)
                .amountInSom(amountSom)
                .provider(PaymentProvider.PIXY)
                .payUrl(payUrl)
                .build();
    }

    @Transactional
    public void activateAccess(Payment payment) {
        log.info("Access faollashtirilmoqda: paymentId={}, userId={}, type={}",
                payment.getId(), payment.getUser().getId(), payment.getPaymentType());

        if (payment.getPaymentType() == PaymentType.SUBSCRIPTION) {
            movieAccessService.updateUserAccessWithSubscription(
                    payment.getUser().getId(),
                    Collections.emptyMap(),
                    true,
                    payment.getSubscriptionDays()
            );
        } else if (payment.getPaymentType() == PaymentType.INDIVIDUAL_SERIES) {
            movieAccessService.grantPaidAccess(
                    payment.getUser().getId(),
                    payment.getSeries().getId(),
                    individualAccessDays
            );
        }

        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        log.info("Access faollashtirildi: paymentId={}", payment.getId());
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    private long calculateSubscriptionPrice(int days) {
        if (days <= 30) return monthlyPriceSom;
        if (days <= 90) return quarterlyPriceSom;
        return yearlyPriceSom;
    }
}
