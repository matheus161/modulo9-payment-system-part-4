package com.eliasnogueira.paymentservice.integration;

import com.eliasnogueira.paymentservice.dto.PaymentRequest;
import com.eliasnogueira.paymentservice.dto.PaymentResponse;
import com.eliasnogueira.paymentservice.dto.PaymentUpdateRequest;
import com.eliasnogueira.paymentservice.model.Payment;
import com.eliasnogueira.paymentservice.model.enums.PaymentSource;
import com.eliasnogueira.paymentservice.model.enums.PaymentStatus;
import com.eliasnogueira.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
    }

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

    @Test
    public void shouldReturn404WhenPaymentNotFound() throws  Exception{
        mockMvc.perform(get("/api/payments/{paymentId}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getPaymentsByPayerId() throws Exception {
        var payerId = UUID.randomUUID();

        var payment1 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.CREDIT_CARD)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();

        var payment2 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("200.00"))
                .status(PaymentStatus.PENDING)
                .build();

       paymentRepository.saveAll(List.of(payment1, payment2));

        var responseInJson = mockMvc.perform(get("/api/payments/payer/{payerId}", payerId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<PaymentResponse> listOfPaymentResponse = mapper.readValue(responseInJson,
                mapper.getTypeFactory().constructCollectionType(List.class, PaymentResponse.class)
        );

        assertThat(listOfPaymentResponse).hasSize(2);
        assertThat(listOfPaymentResponse.get(0).getId()).isNotNull();
        assertThat(listOfPaymentResponse.get(0).getPayerId()).isEqualTo(payerId);
        assertThat(listOfPaymentResponse.get(0).getPaymentSource()).isEqualTo(PaymentSource.CREDIT_CARD);
        assertThat(listOfPaymentResponse.get(0).getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(listOfPaymentResponse.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(listOfPaymentResponse.get(1).getId()).isNotNull();
        assertThat(listOfPaymentResponse.get(1).getPayerId()).isEqualTo(payerId);
        assertThat(listOfPaymentResponse.get(1).getPaymentSource()).isEqualTo(PaymentSource.PIX);
        assertThat(listOfPaymentResponse.get(1).getAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(listOfPaymentResponse.get(1).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    public void shouldUpdatePayment() throws Exception {
        var paymentUpdateRequest = PaymentUpdateRequest.builder().status(PaymentStatus.PAID).build();

        var payment = Payment.builder()
                .payerId(UUID.randomUUID())
                .paymentSource(PaymentSource.CREDIT_CARD)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();

        var savedPayment = paymentRepository.save(payment);

        var responseInJson = mockMvc.perform(put("/api/payments/{paymentId}", savedPayment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(paymentUpdateRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        PaymentResponse paymentResponse = mapper.readValue(responseInJson, PaymentResponse.class);

        assertThat(paymentResponse.getId()).isEqualTo(savedPayment.getId());
        assertThat(paymentResponse.getPayerId()).isEqualTo(savedPayment.getPayerId());
        assertThat(paymentResponse.getPaymentSource()).isEqualTo(savedPayment.getPaymentSource());
        assertThat(paymentResponse.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(paymentResponse.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    public void getAllPayments() throws Exception {
        var payerId = UUID.randomUUID();

        var payment1 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.CREDIT_CARD)
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING)
                .build();

        var payment2 = Payment.builder()
                .payerId(payerId)
                .paymentSource(PaymentSource.PIX)
                .amount(new BigDecimal("200.00"))
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2));

        mockMvc.perform(get("/api/payments", payerId))
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", notNullValue()))
                .andExpect(jsonPath("$[*].payerId", everyItem(is(payerId.toString()))))
                .andExpect(jsonPath("$[*].amount", containsInAnyOrder(100.00, 200.00)));

    }
}
