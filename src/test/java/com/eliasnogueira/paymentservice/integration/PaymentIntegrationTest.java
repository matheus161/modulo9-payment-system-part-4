package com.eliasnogueira.paymentservice.integration;

import com.eliasnogueira.paymentservice.dto.PaymentRequest;
import com.eliasnogueira.paymentservice.model.Payment;
import com.eliasnogueira.paymentservice.model.enums.PaymentSource;
import com.eliasnogueira.paymentservice.model.enums.PaymentStatus;
import com.eliasnogueira.paymentservice.repository.PaymentRepository;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void createPayment() throws Exception {
        String payload = """
                {
                    "payerId": "2df75f27-c46a-43e2-8e70-1145bdd93e7d",
                    "paymentSource": "PIX",
                    "amount": 100.50
                }
                """;
        mockMvc.perform(
                post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payerId", is("2df75f27-c46a-43e2-8e70-1145bdd93e7d")))
                .andExpect(jsonPath("$.paymentSource", is(PaymentSource.PIX.name())))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.status", is(PaymentStatus.PENDING.name())));
    }

    @Test
    public void getPaymentById() throws Exception {
        // Pré-condição para o teste
        var payment = Payment.builder()
                .payerId(UUID.randomUUID())
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();

        var savedPayment = paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/{paymentId}", savedPayment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payerId", is(savedPayment.getPayerId().toString())))
                .andExpect(jsonPath("$.paymentSource", is(savedPayment.getPaymentSource().name())))
                .andExpect(jsonPath("$.amount", is(savedPayment.getAmount().doubleValue())))
                .andExpect(jsonPath("$.status", is(savedPayment.getStatus().name())));
    }
}
