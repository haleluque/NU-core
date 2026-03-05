package com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST request for transfer operation (web input adapter).
 */
public record TransferRequest(
        @NotNull UUID originAccountId,
        @NotNull UUID destinationAccountId,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount
) {}
