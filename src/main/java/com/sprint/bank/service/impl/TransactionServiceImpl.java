package com.sprint.bank.service.impl;

import com.sprint.bank.dto.AmountRequest;
import com.sprint.bank.dto.TransactionResponse;
import com.sprint.bank.dto.TransferRequest;
import com.sprint.bank.entity.Account;
import com.sprint.bank.entity.AccountStatus;
import com.sprint.bank.entity.Transaction;
import com.sprint.bank.entity.TransactionType;
import com.sprint.bank.exception.AccountNotFoundException;
import com.sprint.bank.exception.BadRequestException;
import com.sprint.bank.exception.InsufficientBalanceException;
import com.sprint.bank.repository.AccountRepository;
import com.sprint.bank.repository.TransactionRepository;
import com.sprint.bank.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(AccountRepository accountRepository,
                                  TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransactionResponse deposit(String userEmail, Long accountId, AmountRequest request) {
        Account account = getOwnedActiveAccount(userEmail, accountId);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        String description = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription()
                : "Cash deposit";

        Transaction tx = recordTransaction(account, TransactionType.DEPOSIT, request.getAmount(), description);
        return mapToResponse(tx);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(String userEmail, Long accountId, AmountRequest request) {
        Account account = getOwnedActiveAccount(userEmail, accountId);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + account.getAccountNumber());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        String description = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription()
                : "Cash withdrawal";

        Transaction tx = recordTransaction(account, TransactionType.WITHDRAW, request.getAmount(), description);
        return mapToResponse(tx);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(String userEmail, TransferRequest request) {
        Account fromAccount = getOwnedActiveAccount(userEmail, request.getFromAccountId());
        Account toAccount = findDestinationAccount(request);

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BadRequestException("Cannot transfer money to the same account");
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Destination account is " + toAccount.getStatus() + " and cannot receive transfers");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance for transfer");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String note = (request.getDescription() != null && !request.getDescription().isBlank())
                ? " (" + request.getDescription() + ")"
                : "";

        Transaction debitTx = recordTransaction(
                fromAccount,
                TransactionType.TRANSFER,
                request.getAmount(),
                "Transferred to " + toAccount.getAccountNumber() + note
        );

        recordTransaction(
                toAccount,
                TransactionType.TRANSFER,
                request.getAmount(),
                "Received from " + fromAccount.getAccountNumber() + note
        );

        return mapToResponse(debitTx);
    }

    @Override
    public List<TransactionResponse> getStatement(String userEmail, Long accountId) {
        Account account = getOwnedAccount(userEmail, accountId);

        return transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> filterTransactions(TransactionType type, LocalDate date) {
        // Admin filter implemented using Java Streams
        return transactionRepository.findAll()
                .stream()
                .filter(tx -> type == null || tx.getType() == type)
                .filter(tx -> date == null || tx.getTimestamp().toLocalDate().equals(date))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Account getOwnedAccount(String userEmail, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (!account.getCustomer().getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("You do not own this account");
        }
        return account;
    }

    private Account getOwnedActiveAccount(String userEmail, Long accountId) {
        Account account = getOwnedAccount(userEmail, accountId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Account is " + account.getStatus() + ". Only ACTIVE accounts can perform transactions.");
        }
        return account;
    }

    private Account findDestinationAccount(TransferRequest request) {
        if (request.getToAccountNumber() != null && !request.getToAccountNumber().isBlank()) {
            return accountRepository.findByAccountNumber(request.getToAccountNumber().trim())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getToAccountNumber()));
        }
        if (request.getToAccountId() != null) {
            return accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found with id: " + request.getToAccountId()));
        }
        throw new BadRequestException("Please provide destination account number or ID");
    }

    private Transaction recordTransaction(Account account,
                                          TransactionType type,
                                          java.math.BigDecimal amount,
                                          String description) {
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setDescription(description);
        return transactionRepository.save(tx);
    }

    private TransactionResponse mapToResponse(Transaction tx) {
        TransactionResponse dto = new TransactionResponse();
        dto.setId(tx.getId());
        dto.setType(tx.getType());
        dto.setAmount(tx.getAmount());
        dto.setTimestamp(tx.getTimestamp());
        dto.setDescription(tx.getDescription());
        dto.setAccountNumber(tx.getAccount().getAccountNumber());
        return dto;
    }
}
