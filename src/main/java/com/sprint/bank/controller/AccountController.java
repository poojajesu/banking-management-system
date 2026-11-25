package com.sprint.bank.controller;

import com.sprint.bank.dto.AccountResponse;
import com.sprint.bank.dto.CreateAccountRequest;
import com.sprint.bank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(Authentication authentication,
                                                         @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        List<AccountResponse> accounts = accountService.getMyAccounts(authentication.getName());
        return ResponseEntity.ok(accounts);
    }
}
