package com.example.movieapp.configurer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate payments.provider ustuniga faqat o'sha paytdagi enum qiymatlari (PIXY) bilan CHECK cheklov qo'ygan bo'lishi mumkin.
 * ddl-auto=update uni yangilamaydi, shuning uchun yangi MANUAL qiymati saqlanishi uchun cheklov olib tashlanadi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProviderConstraintFix implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_provider_check");
        } catch (Exception e) {
            log.warn("payments_provider_check cheklovini olib tashlab bo'lmadi: {}", e.getMessage());
        }
    }
}
