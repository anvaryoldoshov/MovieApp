package com.example.movieapp.service;

import com.example.movieapp.entities.Payment;
import com.example.movieapp.entities.Series;
import com.example.movieapp.entities.User;
import com.example.movieapp.enums.PaymentProvider;
import com.example.movieapp.enums.PaymentStatus;
import com.example.movieapp.enums.PaymentType;
import com.example.movieapp.repository.PaymentRepository;
import com.example.movieapp.repository.SeriesRepo;
import com.example.movieapp.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DB'ga tegmasdan (payments jadvalidagi eski check-constraint'dan mustaqil ravishda)
 * PaymentService.activateAccess() xarid tasdiqlanganda UserNotificationService.notifyUser()ni
 * to'g'ri parametrlar bilan chaqirishini tekshiradi.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceNotificationHookTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepo userRepo;
    @Mock
    private SeriesRepo seriesRepo;
    @Mock
    private MovieAccessService movieAccessService;
    @Mock
    private PixyService pixyService;
    @Mock
    private UserNotificationService userNotificationService;

    @Test
    void activateAccessSendsPurchaseSuccessNotification() {
        PaymentService paymentService = new PaymentService(
                paymentRepository, userRepo, seriesRepo, movieAccessService, pixyService, userNotificationService);

        User user = User.builder().id(42L).email("buyer@example.com").build();
        Series series = new Series();
        series.setId(7L);
        series.setTitle("Ajoyib Serial");

        Payment payment = Payment.builder()
                .id(100L)
                .user(user)
                .series(series)
                .amount(500000L)
                .status(PaymentStatus.PENDING)
                .provider(PaymentProvider.PIXY)
                .paymentType(PaymentType.INDIVIDUAL_SERIES)
                .subscriptionDays(30)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.activateAccess(payment);

        verify(movieAccessService).grantPaidAccess(42L, 7L, 30);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(userNotificationService).notifyUser(
                eq(user),
                eq("PURCHASE_SUCCESS"),
                any(String.class),
                eq("\"Ajoyib Serial\" seriali uchun 30 kunlik kirish faollashtirildi"),
                isNull()
        );
    }
}
