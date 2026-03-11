package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Bid;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    Optional<Bid> findFirstByAuctionOrderByAmountDesc(Auction auction);// Найти самую высокую ставку для конкретного аукциона
}