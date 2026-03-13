package ru.urfu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.entity.Transaction;
import ru.urfu.entity.TransactionStatusEnum;
import ru.urfu.entity.TransactionType;
import ru.urfu.entity.User;
import ru.urfu.repository.TransactionRepository;
import ru.urfu.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public PaymentServiceImpl(TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BigDecimal calculateTotalIncome(User user) {
        List<Transaction> incomeTransactions = transactionRepository.findByUserAndStatus(user, TransactionStatusEnum.SUCCESS);
        return incomeTransactions.stream()
                .filter(tx -> tx.getType() == TransactionType.DEPOSIT || tx.getType() == TransactionType.REFUND)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalExpense(User user) {
        List<Transaction> expenseTransactions = transactionRepository.findByUserAndStatus(user, TransactionStatusEnum.SUCCESS);
        return expenseTransactions.stream()
                .filter(tx -> tx.getType() == TransactionType.WITHDRAW || tx.getType() == TransactionType.PURCHASE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }



    @Override
    public List<Transaction> getUserTransactions(User user, TransactionType type, LocalDateTime dateFrom, LocalDateTime dateTo) {
        // Фильтрация по типу и дате будет реализована в репозитории (дополнить метод)
        return transactionRepository.findFilteredTransactions(user, type, dateFrom, dateTo);
    }

    @Override
    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    @Transactional
    public void createDepositRequest(User user, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatusEnum.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription("Пополнение баланса");
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void approveDeposit(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatusEnum.PENDING) {
            return;
        }

        transaction.setStatus(TransactionStatusEnum.SUCCESS);
        transactionRepository.save(transaction);

        User user = transaction.getUser();
        user.setAccountBalance(user.getAccountBalance().add(transaction.getAmount()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void rejectDeposit(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatusEnum.PENDING) {
            return;
        }

        transaction.setStatus(TransactionStatusEnum.FAILED);
        transactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> getPendingDeposits() {
        return transactionRepository.findByTypeAndStatus(
                TransactionType.DEPOSIT,
                TransactionStatusEnum.PENDING
        );
    }
}