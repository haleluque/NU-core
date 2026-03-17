package com.haleluque.nu.core.payment.infrastructure.adapter.input.web;

import com.haleluque.nu.core.payment.application.port.in.ListAccountsPort;
import com.haleluque.nu.core.payment.application.port.in.SeedAccountsPort;
import com.haleluque.nu.core.payment.domain.model.Account;
import com.haleluque.nu.core.payment.infrastructure.adapter.input.web.dto.AccountSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for lab/ops: seed data, list accounts, etc.
 * In production these should be protected or disabled.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final SeedAccountsPort seedAccountsPort;
    private final ListAccountsPort listAccountsPort;

    public AdminController(SeedAccountsPort seedAccountsPort, ListAccountsPort listAccountsPort) {
        this.seedAccountsPort = seedAccountsPort;
        this.listAccountsPort = listAccountsPort;
    }

    @PostMapping("/seed-accounts")
    public ResponseEntity<Map<String, String>> seedAccounts() {
        seedAccountsPort.seedDefaultAccounts();
        return ResponseEntity.ok(Map.of("message", "Default accounts (Alice, Bob, Carol) seeded."));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountSummaryResponse>> listAccounts() {
        List<Account> accounts = listAccountsPort.listAllAccounts();
        List<AccountSummaryResponse> body = accounts.stream()
                .map(a -> new AccountSummaryResponse(a.id(), a.customerName(), a.balance()))
                .toList();
        return ResponseEntity.ok(body);
    }
}
