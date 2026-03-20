package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Goods;

import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findAllByIsActiveTrue();
    List<Auction> findAllByIsActiveTrueAndGoods_User_Id(Long userId);
    Boolean existsByGoodsIdAndIsActiveTrue(Long goodsId);
    boolean existsByGoodsId(Long goodsId);

    // Найти аукционы где пользователь делал ставки
    @Query("SELECT DISTINCT b.auction FROM Bid b WHERE b.bidder.id = :userId ORDER BY b.bidTime DESC")
    List<Auction> findAuctionsWhereUserBid(@Param("userId") Long userId);

    // Найти аукционы где пользователь лидирует
    @Query("SELECT a FROM Auction a WHERE a.currentHighestBidder.id = :userId AND a.isActive = true")
    List<Auction> findAuctionsWhereUserIsLeader(@Param("userId") Long userId);
}
