package ru.urfu.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


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

    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Теперь ссылаемся на общего пользователя
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price") //Цена товара
    private BigDecimal price;


    @Column(name = "quantity") //кол-во товара
    private Long quantity;


    @Column(name = "delivery_status")
    private int deliveryStatus; // 0 - собирается, 1 - в пути...

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status")
    private GoodsStatus moderationStatus = GoodsStatus.PENDING;

    @OneToMany(mappedBy = "goods", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsQuestion> questions = new ArrayList<>();

}
