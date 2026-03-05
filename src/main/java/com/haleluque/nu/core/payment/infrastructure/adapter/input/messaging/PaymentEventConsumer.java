package com.haleluque.nu.core.payment.infrastructure.adapter.input.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Input adapter: consumer for payment events from Kafka.
 */
@Component
public class PaymentEventConsumer {

    @KafkaListener(topics = "payment.requests", groupId = "nu-core-payment")
    public void consume(String message) {
        // Orchestrate with input port according to payload
    }
}
