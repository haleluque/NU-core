package com.haleluque.nu.core.payment.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Input port: transfer orchestration (use case).
 */
public interface TransferPort {

    TransferResult transfer(TransferCommand command);

    Optional<TransferDetailResult> getById(UUID transferId);
}
