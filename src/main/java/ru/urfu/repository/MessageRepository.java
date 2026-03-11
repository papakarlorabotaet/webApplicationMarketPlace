package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.urfu.entity.Message;
import ru.urfu.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.sender = ?1 OR m.receiver = ?1 ORDER BY m.timestamp DESC")
    List<Message> findAllByUser(User user);
}
