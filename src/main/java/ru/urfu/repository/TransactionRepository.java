package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.urfu.entity.*;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Дополнительный метод для фильтрации

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:dateFrom IS NULL OR t.createdAt >= :dateFrom) " +
            "AND (:dateTo IS NULL OR t.createdAt <= :dateTo) " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findFilteredTransactions(
            @Param("user") User user,
            @Param("type") TransactionType type,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    List<Transaction> findByUser(User user);
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);

    List<Transaction> findByTypeAndStatus(TransactionType type, TransactionStatusEnum status);

    List<Transaction> findByUserAndStatus(User user, TransactionStatusEnum status);
}
