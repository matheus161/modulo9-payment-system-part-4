package com.eliasnogueira.paymentservice.unit;

import com.eliasnogueira.paymentservice.exceptions.PaymentLimitException;
import com.eliasnogueira.paymentservice.validator.PaymentLimitValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

public class PaymentLimitValidatorTest {

    @Test
    void shouldBeWithinLimit() {
        BigDecimal limit = new BigDecimal("500.00");
        boolean isWithinLimit = PaymentLimitValidator.isWithinLimit(limit);
        assertThat(isWithinLimit).isTrue();
    }

    @Test
    void shouldNotAcceptGreaterThanLimit() {
        BigDecimal limit = new BigDecimal("2500.00");
        boolean isWithinLimit = PaymentLimitValidator.isWithinLimit(limit);
        assertThat(isWithinLimit).isFalse();
    }


    @Test
    void shouldNotAcceptNullAmount() {
        boolean isWithinLimit = PaymentLimitValidator.isWithinLimit(null);
        assertThat(isWithinLimit).isFalse();
    }


    @Test
    void shouldNotAcceptNegativeAmount() {
        BigDecimal limit = new BigDecimal("-500.00");
        assertThatThrownBy(() -> PaymentLimitValidator.isWithinLimit(limit))
                .isInstanceOf(PaymentLimitException.class)
                .hasMessage("Amount must be greater than zero");
    }

    @Test
    void shouldNotAcceptZeroAmount() {
        assertThatThrownBy(() -> PaymentLimitValidator.isWithinLimit(BigDecimal.ZERO))
                .isInstanceOf(PaymentLimitException.class)
                .hasMessage("Amount must be greater than zero");
    }

    @Test
    void shouldAcceptAmountBelowTheLimit() {
        BigDecimal limit = new BigDecimal("1999.00");
        boolean isWithinLimit = PaymentLimitValidator.isWithinLimit(limit);
        assertThat(isWithinLimit).isTrue();
    }

    @Test
    void shouldAcceptAmountEqualToLimit() {
        BigDecimal limit = new BigDecimal("2000.00");
        boolean isWithinLimit = PaymentLimitValidator.isWithinLimit(limit);
        assertThat(isWithinLimit).isTrue();
    }

    @Test
    void shouldNotAcceptOutOfLimit() {
        BigDecimal limit = new BigDecimal("2000.01");
        boolean isWithinLimit = PaymentLimitValidator.isWithinLimit(limit);
        assertThat(isWithinLimit).isFalse();
    }
}
