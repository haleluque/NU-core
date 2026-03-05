package com.haleluque.nu.core.payment.application.port.out;

import com.haleluque.nu.core.payment.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for transaction persistence (payment saga).
 */
public interface TransactionRepository {

    Optional<Transaction> findById(UUID id);

    void save(Transaction transaction);
}
