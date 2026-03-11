package ru.urfu.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime orderDate;

    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private User buyer;           // покупатель (может быть null, если заказ импортирован)

    @ManyToOne
    @JoinColumn(name = "goods_id")
    private Goods goods;          // товар

    private int quantity;

    private BigDecimal totalPrice;

    private String status;        // например, "Оплачен", "Отправлен"

}
