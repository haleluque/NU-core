package com.haleluque.nu.core.payment.application.exception;

/**
 * Application exception: insufficient balance for the operation.
 */
public final class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Insufficient balance");
    }
}
