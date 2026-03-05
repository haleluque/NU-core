package com.haleluque.nu.core.payment.application.port.in;

import java.util.UUID;

/**
 * Result of a transfer (input port).
 */
public record TransferResult(
        UUID transferId,
        UUID originAccountId,
        UUID destinationAccountId,
        String status
) {}
