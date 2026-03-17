package com.haleluque.nu.core.payment.application.usecase;

import com.haleluque.nu.core.payment.application.port.in.ListAccountsPort;
import com.haleluque.nu.core.payment.application.port.out.AccountRepository;
import com.haleluque.nu.core.payment.domain.model.Account;

import java.util.List;

/**
 * Use case: list all accounts (admin).
 */
public class ListAccountsUseCase implements ListAccountsPort {

    private final AccountRepository accountRepository;

    public ListAccountsUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public List<Account> listAllAccounts() {
        return accountRepository.findAll();
    }
}
