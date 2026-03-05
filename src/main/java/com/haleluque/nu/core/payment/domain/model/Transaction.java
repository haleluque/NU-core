package com.haleluque.nu.core.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a transaction (payment saga).
 */
public record Transaction(
        UUID id,
        UUID accountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String type,
        String status,
        Instant createdAt
) {
    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED_BY_RISK = "CANCELLED_BY_RISK";

    public Transaction {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public Transaction withStatus(String newStatus) {
        return new Transaction(id, accountId, destinationAccountId, amount, type, newStatus, createdAt);
    }
}
