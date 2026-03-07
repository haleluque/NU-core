package com.haleluque.nu.core.payment.infrastructure.adapter.input.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.application.port.out.TransactionRepository;
import com.haleluque.nu.core.payment.domain.model.Account;
import com.haleluque.nu.core.payment.domain.model.Transaction;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.messaging.dto.RiskEventPayload;
import com.haleluque.nu.core.payment.infrastructure.config.PaymentAppProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Consumer for risk-engine responses (topic risk-events).
 * APPROVED: updates transaction to COMPLETED.
 * REJECTED: compensation (refund to origin account, transaction CANCELLED_BY_RISK).
 */
@Component
public class RiskResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(RiskResponseConsumer.class);

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PaymentAppProperties appProperties;
    private final MeterRegistry meterRegistry;
    private final Counter processedCounter;

    public RiskResponseConsumer(TransactionRepository transactionRepository,
                                AccountRepository accountRepository,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry,
                                PaymentAppProperties appProperties) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().registerModule(new JavaTimeModule());
        this.appProperties = appProperties;
        this.meterRegistry = meterRegistry;
        this.processedCounter = meterRegistry.counter(appProperties.getMetrics().getPaymentsProcessedTotal());
    }

    @KafkaListener(topics = "${app.payment.kafka.topic-risk-events:risk-events}", groupId = "nu-core-payment-risk-response")
    public void consume(String message) {
        String topic = appProperties.getKafka().getTopicRiskEvents();
        log.info("Received risk response from topic {}: {}", topic, message);

        RiskEventPayload payload = parsePayload(message);
        if (payload == null) {
            return;
        }

        UUID transactionId = parseUuid(payload.transactionId());
        if (transactionId == null) {
            log.warn("Invalid transactionId in risk response: {}", payload.transactionId());
            return;
        }

        if (RiskEventPayload.APPROVED.equalsIgnoreCase(payload.status())) {
            processedCounter.increment();
            handleApproved(transactionId);
        } else if (RiskEventPayload.REJECTED.equalsIgnoreCase(payload.status())) {
            processedCounter.increment();
            handleRejected(transactionId, payload);
        } else {
            log.warn("Unknown risk status '{}' for transactionId={}, ignoring", payload.status(), transactionId);
        }
    }

    private void handleApproved(UUID transactionId) {
        Optional<Transaction> opt = transactionRepository.findById(transactionId);
        if (opt.isEmpty()) {
            log.warn("Transaction not found for APPROVED: {}", transactionId);
            return;
        }
        Transaction tx = opt.get();
        Transaction updated = tx.withStatus(Transaction.COMPLETED);
        transactionRepository.save(updated);
        log.info("Transaction {} updated to COMPLETED (risk APPROVED)", transactionId);
    }

    private void handleRejected(UUID transactionId, RiskEventPayload payload) {
        Optional<Transaction> optTx = transactionRepository.findById(transactionId);
        if (optTx.isEmpty()) {
            log.warn("Transaction not found for REJECTED compensation: {}", transactionId);
            return;
        }
        Transaction tx = optTx.get();

        Optional<Account> optAccount = accountRepository.findById(tx.accountId());
        if (optAccount.isEmpty()) {
            log.error("Origin account not found for compensation, transactionId={}, accountId={}",
                    transactionId, tx.accountId());
            return;
        }

        Account origin = optAccount.get();
        Account refunded = origin.credit(tx.amount());
        accountRepository.save(refunded);

        Transaction cancelled = tx.withStatus(Transaction.CANCELLED_BY_RISK);
        transactionRepository.save(cancelled);

        meterRegistry.counter(appProperties.getMetrics().getPaymentsCompensatedTotal(), "reason", "fraud").increment();

        log.info("Compensation applied for transaction {}: account {} credited {}, transaction status CANCELLED_BY_RISK (reason: {})",
                transactionId, tx.accountId(), tx.amount(), payload.reason());
    }

    private RiskEventPayload parsePayload(String raw) {
        if (raw == null || raw.isBlank()) {
            log.warn("Invalid risk response: null or blank payload");
            return null;
        }
        try {
            return objectMapper.readValue(raw, RiskEventPayload.class);
        } catch (Exception e) {
            log.error("Invalid risk response format: {}", e.getMessage(), e);
            return null;
        }
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
