package com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST response for an account summary (id, customer name, balance).
 */
public record AccountSummaryResponse(
        UUID id,
        String customerName,
        BigDecimal balance
) {}
