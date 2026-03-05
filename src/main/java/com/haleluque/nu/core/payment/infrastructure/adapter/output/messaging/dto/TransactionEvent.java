package com.haleluque.nu.core.payment.infrastructure.adapter.output.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event sent to topic payment-events to start risk validation.
 */
public record TransactionEvent(
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("accountId") UUID accountId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("status") String status
) {
    @JsonCreator
    public TransactionEvent {
    }
}
