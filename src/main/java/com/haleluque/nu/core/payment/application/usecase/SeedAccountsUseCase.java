package com.haleluque.nu.core.payment.application.usecase;

import com.haleluque.nu.core.payment.application.port.in.SeedAccountsPort;
import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.domain.model.Account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Seeds the three default accounts (Alice, Bob, Carol). Idempotent.
 */
public class SeedAccountsUseCase implements SeedAccountsPort {

    private final AccountRepository accountRepository;

    public SeedAccountsUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void seedDefaultAccounts() {
        accountRepository.save(new Account(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Alice",
                new BigDecimal("5000.00")));
        accountRepository.save(new Account(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Bob",
                new BigDecimal("3000.50")));
        accountRepository.save(new Account(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "Carol",
                new BigDecimal("10000.00")));
    }
}
