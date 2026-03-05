package com.haleluque.nu.core.payment.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Input command for transfer (input port).
 */
public record TransferCommand(
        UUID originAccountId,
        UUID destinationAccountId,
        BigDecimal amount
) {}
