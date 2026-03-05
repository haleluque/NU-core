package com.haleluque.nu.core.payment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable account. credit and debit return new instances.
 */
public record Account(UUID id, String customerName, BigDecimal balance) {

    public Account {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(customerName, "customerName cannot be null");
        Objects.requireNonNull(balance, "balance cannot be null");
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("balance cannot be negative");
        }
    }

    public Account credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        return new Account(id, customerName, balance.add(amount));
    }

    public Account debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        return new Account(id, customerName, balance.subtract(amount));
    }
}
