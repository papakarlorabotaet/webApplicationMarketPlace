package ru.urfu.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // A7X9-K2M4-P8Q1

    private BigDecimal discountAmount; // Фиксированная скидка
    private Integer discountPercent;   // Процентная скидка

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private Integer usageLimit;        // 0 = безлимит
    private Integer usageCount = 0;

    private boolean active = true;
}