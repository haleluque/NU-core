package com.haleluque.nu.core.payment.application.exception;

import java.util.UUID;

/**
 * Application exception: account not found.
 */
public final class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
}
