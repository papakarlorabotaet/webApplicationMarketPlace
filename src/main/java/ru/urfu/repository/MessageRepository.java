package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.urfu.entity.Message;
import ru.urfu.entity.User;

import javax.transaction.Transactional;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.sender = ?1 OR m.receiver = ?1 ORDER BY m.timestamp DESC")
    List<Message> findAllByUser(User user);

    @Query("SELECT m FROM Message m WHERE (m.sender = ?1 AND m.receiver = ?2) OR (m.sender = ?2 AND m.receiver = ?1) ORDER BY m.timestamp ASC")
    List<Message> findChatHistory(User user1, User user2);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender = ?1 AND m.receiver = ?2 AND m.isRead = false")
    void markAsRead(User sender, User receiver);

    @Query(value = "SELECT * FROM messages WHERE id IN (" +
            "SELECT MAX(id) FROM messages " +
            "WHERE sender_id = :userId OR receiver_id = :userId " +
            "GROUP BY CASE " +
            "  WHEN sender_id = :userId THEN receiver_id " +
            "  ELSE sender_id " +
            "END) ORDER BY timestamp DESC", nativeQuery = true)
    List<Message> findAllRecentDialogs(@Param("userId") Long userId);

    long countByReceiverAndIsReadFalse(User receiver);

}
