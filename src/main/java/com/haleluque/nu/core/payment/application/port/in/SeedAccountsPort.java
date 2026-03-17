package com.haleluque.nu.core.payment.application.port.in;

/**
 * Input port to seed default payment accounts (e.g. Alice, Bob, Carol).
 * Used by admin/ops to load initial data after tables exist.
 */
public interface SeedAccountsPort {

    /**
     * Inserts or overwrites the default seed accounts. Idempotent.
     */
    void seedDefaultAccounts();
}
