package com.haleluque.nu.core.payment.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Detail result for a transfer by id (input port).
 */
public record TransferDetailResult(
        UUID transferId,
        UUID originAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {}
