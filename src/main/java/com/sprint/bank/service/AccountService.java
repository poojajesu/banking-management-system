package com.sprint.bank.service;

import com.sprint.bank.dto.AccountResponse;
import com.sprint.bank.dto.CreateAccountRequest;
import com.sprint.bank.dto.CustomerResponse;
import com.sprint.bank.entity.AccountStatus;

import java.util.List;

public interface AccountService {
    AccountResponse createAccount(String userEmail, CreateAccountRequest request);
    List<AccountResponse> getMyAccounts(String userEmail);
    List<CustomerResponse> getAllCustomers();
    List<AccountResponse> getAllAccounts();
    AccountResponse updateAccountStatus(Long accountId, AccountStatus status);
}
