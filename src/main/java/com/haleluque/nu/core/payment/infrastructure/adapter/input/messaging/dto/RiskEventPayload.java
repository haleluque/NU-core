package com.haleluque.nu.core.payment.infrastructure.adapter.input.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO for messages received from topic risk-events (risk-engine decision).
 */
public record RiskEventPayload(
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("status") String status,
        @JsonProperty("reason") String reason,
        @JsonProperty("timestamp") Instant timestamp
) {
    @JsonCreator
    public RiskEventPayload {
    }

    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
}
