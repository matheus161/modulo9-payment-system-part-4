package com.eliasnogueira.paymentservice.integration;

import com.eliasnogueira.paymentservice.model.Payment;
import com.eliasnogueira.paymentservice.model.enums.PaymentSource;
import com.eliasnogueira.paymentservice.model.enums.PaymentStatus;
import com.eliasnogueira.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class PaymentDataIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSumDailyPaymentsByPayerId() {
        UUID payerId = UUID.randomUUID();

        var payment1 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();

        var payment2 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("300.00"))
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2));

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        var total = paymentRepository.sumPaymentsByPayerIdAndDate(payerId, startOfDay, endOfDay);

        assertThat(total).isEqualTo(new BigDecimal("400.00"));
    }

    @Test
    void shouldNotSumPaymentsFromDifferentDays() {
        UUID payerId = UUID.randomUUID();

        var payment1 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();

        var payment2 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("300.00"))
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterday = startOfDay.minusDays(1).plusDays(10);

        entityManager.getEntityManager()
                .createQuery("UPDATE Payment p SET p.createdAt = :createdAt WHERE p.id = :id")
                .setParameter("createdAt", yesterday)
                .setParameter("id", payment1.getId())
                .executeUpdate();

        entityManager.clear();

        var total = paymentRepository.sumPaymentsByPayerIdAndDate(payerId, startOfDay, endOfDay);

        assertThat(total).isEqualTo(new BigDecimal("300.00"));
    }
}
