package ru.urfu.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "goods_questions")
public class GoodsQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с товаром
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    // Связь с покупателем (автором вопроса)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 1000)
    private String text; // Текст вопроса

    @Column(length = 1000)
    private String answer; // Ответ продавца (можно оставить пустым до ответа)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GoodsQuestionStatus status = GoodsQuestionStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}