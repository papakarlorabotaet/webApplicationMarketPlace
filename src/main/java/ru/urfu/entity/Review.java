package ru.urfu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "goods_id")
    private Goods goods;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author; // Покупатель

    @Column(nullable = false)
    private int rating; // Оценка от 1 до 5

    @Column(length = 1000)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}