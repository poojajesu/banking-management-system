package com.sprint.bank.service;

import com.sprint.bank.dto.AmountRequest;
import com.sprint.bank.dto.TransactionResponse;
import com.sprint.bank.dto.TransferRequest;
import com.sprint.bank.entity.TransactionType;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {
    TransactionResponse deposit(String userEmail, Long accountId, AmountRequest request);
    TransactionResponse withdraw(String userEmail, Long accountId, AmountRequest request);
    TransactionResponse transfer(String userEmail, TransferRequest request);
    List<TransactionResponse> getStatement(String userEmail, Long accountId);
    List<TransactionResponse> filterTransactions(TransactionType type, LocalDate date);
}
