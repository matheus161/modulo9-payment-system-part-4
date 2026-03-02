package com.eliasnogueira.paymentservice.unit;

import com.eliasnogueira.paymentservice.dto.PaymentRequest;
import com.eliasnogueira.paymentservice.exceptions.PaymentLimitException;
import com.eliasnogueira.paymentservice.model.enums.PaymentSource;
import com.eliasnogueira.paymentservice.model.enums.PaymentStatus;
import com.eliasnogueira.paymentservice.repository.PaymentRepository;
import com.eliasnogueira.paymentservice.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Should save a payment when limit is not exceeded")
    void shouldSavePaymentWhenLimitIsNotExceeded() {
        when(paymentRepository.sumPaymentsByPayerIdAndDate(any(), any(), any()))
                .thenReturn(new BigDecimal("200.00"));

        when(paymentRepository.save(any()))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        var paymentRequest = PaymentRequest.builder()
                .payerId(UUID.randomUUID())
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("100.50"))
                .build();

        var createdPayment = paymentService.createPayment(paymentRequest);
        assertThat(createdPayment.getPayerId()).isEqualTo(paymentRequest.getPayerId());
        assertThat(createdPayment.getPaymentSource()).isEqualTo(PaymentSource.PIX);
        assertThat(createdPayment.getAmount()).isEqualByComparingTo(paymentRequest.getAmount());
        assertThat(createdPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(paymentRepository).save(any());
    }

    @Test
    @DisplayName("Should not save a payment when limit is exceeded")
    void shouldNotSavePaymentWhenLimitIsExceeded() {
        when(paymentRepository.sumPaymentsByPayerIdAndDate(any(), any(), any()))
                .thenReturn(new BigDecimal("200.00"));

        var paymentRequest = PaymentRequest.builder()
                .payerId(UUID.randomUUID())
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("2500.00"))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest))
                .isInstanceOf(PaymentLimitException.class)
                .hasMessageContaining("Daily payment limit exceeded for source:");
    }

    @Test
    @DisplayName("Should not save payment when amount is zero")
    void shouldSavePaymentWhenAmountIsZero() {
        var paymentRequest = PaymentRequest.builder()
                .payerId(UUID.randomUUID())
                .paymentSource(PaymentSource.PIX)
                .amount(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest))
                .isInstanceOf(PaymentLimitException.class)
                .hasMessage("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should not save payment when amount is negative")
    void shouldNotSavePaymentWhenAmountIsNegative() {
        var paymentRequest = PaymentRequest.builder()
                .payerId(UUID.randomUUID())
                .paymentSource(PaymentSource.PIX)
                .amount(new  BigDecimal("-2500.00"))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment(paymentRequest))
                .isInstanceOf(PaymentLimitException.class)
                .hasMessage("Amount must be greater than zero");
    }

}
