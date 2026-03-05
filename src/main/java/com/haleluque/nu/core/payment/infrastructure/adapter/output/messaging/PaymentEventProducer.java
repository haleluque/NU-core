package com.haleluque.nu.core.payment.infrastructure.adapter.output.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.haleluque.nu.core.payment.infrastructure.adapter.output.messaging.dto.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Output adapter: Kafka producer for payment events.
 */
@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String TOPIC_PAYMENT_EVENTS = "payment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper == null ? defaultObjectMapper() : objectMapper;
    }

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this(kafkaTemplate, null);
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Sends a TransactionEvent to topic payment-events to start risk validation.
     */
    public void sendTransactionEvent(@NonNull TransactionEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_PAYMENT_EVENTS, event.transactionId(), payload);
            log.info("Transaction event sent to {}: transactionId={}, accountId={}, amount={}, status={}",
                    TOPIC_PAYMENT_EVENTS, event.transactionId(), event.accountId(), event.amount(), event.status());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TransactionEvent for transactionId={}", event.transactionId(), e);
        }
    }

    public void sendPaymentCompleted(@NonNull String key, @NonNull String payload) {
        kafkaTemplate.send("payment.completed", key, payload);
    }
}
