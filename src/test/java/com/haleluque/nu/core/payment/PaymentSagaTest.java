package com.haleluque.nu.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto.TransferRequest;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto.TransferResponse;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.messaging.dto.RiskEventPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the payment saga: transfer initiated, risk rejects, compensation applied.
 * Verifies that the origin account balance in DynamoDB returns to the original value after rejection.
 */
@AutoConfigureWebTestClient
class PaymentSagaTest extends AbstractIntegrationTest {

    private static final UUID ORIGIN_ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DESTINATION_ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("100.00");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.payment.kafka.topic-risk-events:risk-events}")
    private String topicRiskEvents;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void whenRiskRejects_thenCompensationRestoresOriginBalance() throws Exception {
        BigDecimal balanceBefore = accountRepository.findById(ORIGIN_ACCOUNT_ID).orElseThrow().balance();

        EntityExchangeResult<TransferResponse> result = webTestClient
                .post()
                .uri("/api/v1/payments/transfer")
                .bodyValue(new TransferRequest(ORIGIN_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, TRANSFER_AMOUNT))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransferResponse.class)
                .returnResult();

        TransferResponse response = Objects.requireNonNull(result.getResponseBody(), "transfer response body");
        UUID transferId = Objects.requireNonNull(response.transferId(), "transferId");
        assertThat(response.status()).isEqualTo("PENDING_RISK");

        RiskEventPayload rejection = new RiskEventPayload(
                transferId.toString(),
                RiskEventPayload.REJECTED,
                "fraud",
                Instant.now()
        );
        String payload = Objects.requireNonNull(objectMapper.writeValueAsString(rejection), "serialized payload");
        kafkaTemplate.send(topicRiskEvents, payload);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    BigDecimal currentBalance = accountRepository.findById(ORIGIN_ACCOUNT_ID).orElseThrow().balance();
                    assertThat(currentBalance).isEqualByComparingTo(balanceBefore);
                });
    }
}
