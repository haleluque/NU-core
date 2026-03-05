package com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto;

import java.util.UUID;

/**
 * REST response for a successful transfer (web input adapter).
 */
public record TransferResponse(
        UUID transferId,
        UUID originAccountId,
        UUID destinationAccountId,
        String status
) {}
