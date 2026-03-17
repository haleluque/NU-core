package com.haleluque.nu.core.payment.application.port.out;

import com.haleluque.nu.core.payment.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for account persistence. Implemented by infrastructure adapters.
 */
public interface AccountRepository {

    Optional<Account> findById(UUID id);

    List<Account> findAll();

    void save(Account account);
}
