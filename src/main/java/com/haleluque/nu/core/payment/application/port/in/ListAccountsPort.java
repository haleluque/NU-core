package com.haleluque.nu.core.payment.application.port.in;

import com.haleluque.nu.core.payment.domain.model.Account;

import java.util.List;

/**
 * Input port: list all accounts (admin/lab).
 */
public interface ListAccountsPort {

    List<Account> listAllAccounts();
}
