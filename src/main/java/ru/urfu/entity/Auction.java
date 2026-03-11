package ru.urfu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "auctions")
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods; // Товар, выставленный на аукцион

    private int startingPrice;
    private int currentHighestBid;

    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;

    private boolean isActive = true;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<Bid> bids;
}