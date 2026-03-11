package ru.urfu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "goods_id")
    private Goods goods; // Привязка к конкретному товару, который обсуждают

    @Column(length = 1000, nullable = false)
    private String content;

    private LocalDateTime timestamp = LocalDateTime.now();

    private boolean isRead = false; // Статус прочтения
}