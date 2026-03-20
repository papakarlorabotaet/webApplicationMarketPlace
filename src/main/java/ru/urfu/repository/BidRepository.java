package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Bid;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    Optional<Bid> findFirstByAuctionOrderByAmountDesc(Auction auction);// Найти самую высокую ставку для конкретного аукциона
    List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId); // Получить все ставки по аукциону, отсортированные по времени (свежие первые)
    Bid findFirstByAuctionIdOrderByBidTimeDesc(Long auctionId);    //Получить последнюю ставку по аукциону
    long countByAuctionId(Long auctionId);// Получить количество ставок по аукциону
}