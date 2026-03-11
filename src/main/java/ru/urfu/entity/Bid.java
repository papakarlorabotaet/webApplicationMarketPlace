package ru.urfu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "bids")//ставка
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder; // Покупатель, сделавший ставку

    private int amount; // Сумма ставки

    private LocalDateTime bidTime = LocalDateTime.now();
}