package ru.urfu.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Goods") //товары
public class Goods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Теперь ссылаемся на общего пользователя
    private User user;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price") //Цена товара
    private int price;

    @Column(name = "delivery_status")
    private int deliveryStatus; // 0 - собирается, 1 - в пути...

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status")
    private GoodsStatus moderationStatus = GoodsStatus.PENDING;
}
