package com.haleluque.nu.core.payment.domain.service;

import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.domain.model.Account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orchestrates a transfer between two accounts, validating sufficient balance.
 * Pure business logic; no framework dependencies.
 */
public final class TransferService {

    private final AccountRepository accountRepository;

    public TransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Transfers amount from originAccountId to destinationAccountId.
     *
     * @throws IllegalArgumentException if either account does not exist or origin has insufficient balance
     */
    public void transfer(UUID originAccountId, UUID destinationAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        Account origin = accountRepository.findById(originAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Origin account not found: " + originAccountId));
        Account destination = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + destinationAccountId));

        Account debitedOrigin = origin.debit(amount);
        Account creditedDestination = destination.credit(amount);

        accountRepository.save(debitedOrigin);
        accountRepository.save(creditedDestination);
    }
}
