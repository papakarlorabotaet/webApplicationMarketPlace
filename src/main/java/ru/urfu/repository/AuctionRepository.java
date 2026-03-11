package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Goods;

import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findAllByIsActiveTrue();
}
