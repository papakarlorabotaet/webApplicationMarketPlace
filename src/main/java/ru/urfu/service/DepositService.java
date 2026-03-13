package ru.urfu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.entity.*;
import ru.urfu.repository.TransactionRepository;
import ru.urfu.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DepositService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    public DepositService(TransactionRepository transactionRepository, UserService userService, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Transaction createDepositRequest(User user, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatusEnum.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription("Пополнение баланса");
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void approveDeposit(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Транзакция не найдена"));
        if (transaction.getStatus() != TransactionStatusEnum.PENDING) {
            throw new RuntimeException("Транзакция не в статусе ожидания");
        }
        transaction.setStatus(TransactionStatusEnum.SUCCESS);
        transactionRepository.save(transaction);

        // Обновляем баланс пользователя
        User user = transaction.getUser();
        user.setAccountBalance(user.getAccountBalance().add(transaction.getAmount()));
        userRepository.save(user);
    }

    @Transactional
    public void rejectDeposit(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Транзакция не найдена"));
        transaction.setStatus(TransactionStatusEnum.FAILED);
        transactionRepository.save(transaction);
    }



    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Transaction> getPendingDeposits() {
        return transactionRepository.findByTypeAndStatus(TransactionType.DEPOSIT, TransactionStatusEnum.PENDING);
    }
}