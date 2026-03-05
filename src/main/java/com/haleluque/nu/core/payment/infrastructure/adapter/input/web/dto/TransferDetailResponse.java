package com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * REST response for a transfer detail (get by id).
 */
public record TransferDetailResponse(
        UUID transferId,
        UUID originAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {}
