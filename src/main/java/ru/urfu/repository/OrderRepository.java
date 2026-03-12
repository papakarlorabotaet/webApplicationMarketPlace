package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.urfu.entity.Order;
import ru.urfu.entity.User;
import ru.urfu.entity.Goods;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.goods g WHERE g.user = :seller")
    List<Order> findByGoodsUser(@Param("seller") User seller);
    boolean existsByBuyerAndGoods(User consumer,  Goods goods);

}
