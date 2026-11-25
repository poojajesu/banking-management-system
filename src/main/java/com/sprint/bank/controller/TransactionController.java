package com.sprint.bank.controller;

import com.sprint.bank.dto.AmountRequest;
import com.sprint.bank.dto.TransactionResponse;
import com.sprint.bank.dto.TransferRequest;
import com.sprint.bank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/accounts/{id}/deposit")
    public ResponseEntity<TransactionResponse> deposit(Authentication authentication,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(transactionService.deposit(authentication.getName(), id, request));
    }

    @PostMapping("/accounts/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(Authentication authentication,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(transactionService.withdraw(authentication.getName(), id, request));
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(Authentication authentication,
                                                        @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.transfer(authentication.getName(), request));
    }

    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getStatement(Authentication authentication,
                                                                  @PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getStatement(authentication.getName(), id));
    }
}
