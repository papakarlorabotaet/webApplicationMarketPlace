package ru.urfu.service;

import org.springframework.stereotype.Service;
import ru.urfu.entity.Transaction;
import ru.urfu.entity.TransactionType;
import ru.urfu.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public interface PaymentService {
    Object calculateTotalIncome(User user);

    Object calculateTotalExpense(User user);


    List<Transaction> getUserTransactions(User user, TransactionType type, LocalDateTime dateFrom, LocalDateTime dateTo);
    List<Transaction> getUserTransactions(User user);

    void createDepositRequest(User user, BigDecimal amount);
    void approveDeposit(Long transactionId);
    void rejectDeposit(Long transactionId);

    Object getPendingDeposits();
}
