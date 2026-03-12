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
    List<Order> findOrdersByGoodsUser(@Param("seller") User seller);

    // Добавляем @Query, так как товар теперь лежит внутри коллекции items
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o JOIN o.items i WHERE o.buyer = :consumer AND i.goods = :goods")
    boolean existsByBuyerAndGoods(@Param("consumer") User consumer, @Param("goods") Goods goods);

    // Добавьте этот метод, он понадобится для шага 2
    List<Order> findByBuyer(User buyer);

}