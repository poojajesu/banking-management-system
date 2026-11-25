package com.sprint.bank.service.impl;

import com.sprint.bank.dto.AccountResponse;
import com.sprint.bank.dto.CreateAccountRequest;
import com.sprint.bank.dto.CustomerResponse;
import com.sprint.bank.entity.*;
import com.sprint.bank.exception.AccountNotFoundException;
import com.sprint.bank.exception.BadRequestException;
import com.sprint.bank.repository.AccountRepository;
import com.sprint.bank.repository.CustomerProfileRepository;
import com.sprint.bank.repository.TransactionRepository;
import com.sprint.bank.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TransactionRepository transactionRepository;
    private final Random random = new Random();

    public AccountServiceImpl(AccountRepository accountRepository,
                              CustomerProfileRepository customerProfileRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(String userEmail, CreateAccountRequest request) {
        CustomerProfile customer = customerProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("Customer profile not found for user: " + userEmail));

        BigDecimal initialBalance = request.getInitialDeposit() != null
                ? request.getInitialDeposit()
                : BigDecimal.ZERO;

        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setType(request.getType());
        account.setBalance(initialBalance);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        account.setCustomer(customer);

        Account savedAccount = accountRepository.save(account);

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction initialDeposit = new Transaction();
            initialDeposit.setAccount(savedAccount);
            initialDeposit.setType(TransactionType.DEPOSIT);
            initialDeposit.setAmount(initialBalance);
            initialDeposit.setTimestamp(LocalDateTime.now());
            initialDeposit.setDescription("Initial deposit on account opening");
            transactionRepository.save(initialDeposit);
        }

        return mapToAccountResponse(savedAccount);
    }

    @Override
    public List<AccountResponse> getMyAccounts(String userEmail) {
        CustomerProfile customer = customerProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("Customer profile not found"));

        return accountRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerProfileRepository.findAll()
                .stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, AccountStatus status) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        account.setStatus(status);
        Account updated = accountRepository.save(account);
        return mapToAccountResponse(updated);
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            int randomDigits = 100000 + random.nextInt(900000);
            number = "BNK" + randomDigits;
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private AccountResponse mapToAccountResponse(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setType(account.getType());
        dto.setBalance(account.getBalance());
        dto.setStatus(account.getStatus());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setCustomerName(account.getCustomer().getFullName());
        return dto;
    }

    private CustomerResponse mapToCustomerResponse(CustomerProfile profile) {
        CustomerResponse dto = new CustomerResponse();
        dto.setId(profile.getId());
        dto.setFullName(profile.getFullName());
        dto.setEmail(profile.getUser().getEmail());
        dto.setPhone(profile.getPhone());
        dto.setAddress(profile.getAddress());
        dto.setPan(profile.getPan());
        dto.setAccounts(
                profile.getAccounts()
                        .stream()
                        .map(this::mapToAccountResponse)
                        .collect(Collectors.toList())
        );
        return dto;
    }
}
