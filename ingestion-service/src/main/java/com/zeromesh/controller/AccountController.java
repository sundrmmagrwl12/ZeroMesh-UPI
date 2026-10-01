package com.zeromesh.controller;

import com.zeromesh.model.Account;
import com.zeromesh.repository.AccountRepository;
import com.zeromesh.repository.TransactionLedgerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final TransactionLedgerRepository ledgerRepository;

    public AccountController(AccountRepository accountRepository,
                             TransactionLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository  = ledgerRepository;
    }

    // Returns all accounts with current balances
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountRepository.findAll());
    }

    // Returns a single account by UPI ID
    @GetMapping("/{upiId}")
    public ResponseEntity<Account> getAccount(@PathVariable String upiId) {
        Account account = accountRepository.findByUpiId(upiId)
                .orElseThrow(() -> new com.zeromesh.exception.AccountNotFoundException(upiId));
        return ResponseEntity.ok(account);
    }

    // Full transaction ledger — all settled + rejected entries
    @GetMapping("/ledger")
    public ResponseEntity<?> getLedger() {
        return ResponseEntity.ok(ledgerRepository.findAll());
    }

    // Resets demo account balances: Sundram ₹2000, Rahul ₹1000, Priya ₹1500
    @PostMapping("/reset")
    public ResponseEntity<List<Account>> resetBalances() {
        accountRepository.findByUpiId("sundram@upi").ifPresent(a -> {
            a.setBalance(new BigDecimal("2000.00"));
            accountRepository.save(a);
        });
        accountRepository.findByUpiId("rahul@upi").ifPresent(a -> {
            a.setBalance(new BigDecimal("1000.00"));
            accountRepository.save(a);
        });
        accountRepository.findByUpiId("priya@upi").ifPresent(a -> {
            a.setBalance(new BigDecimal("1500.00"));
            accountRepository.save(a);
        });
        return ResponseEntity.ok(accountRepository.findAll());
    }

    // Clears all ledger entries — useful for demo reset
    @PostMapping("/ledger/clear")
    public ResponseEntity<Map<String, String>> clearLedger() {
        ledgerRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "Ledger cleared successfully"));
    }
}
