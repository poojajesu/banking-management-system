package com.sprint.bank.controller;

import com.sprint.bank.dto.AccountResponse;
import com.sprint.bank.dto.CustomerResponse;
import com.sprint.bank.dto.TransactionResponse;
import com.sprint.bank.dto.UpdateStatusRequest;
import com.sprint.bank.entity.TransactionType;
import com.sprint.bank.service.AccountService;
import com.sprint.bank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AdminController(AccountService accountService,
                           TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(accountService.getAllCustomers());
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PutMapping("/accounts/{id}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(accountService.updateAccountStatus(id, request.getStatus()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> filterTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(transactionService.filterTransactions(type, date));
    }
}
