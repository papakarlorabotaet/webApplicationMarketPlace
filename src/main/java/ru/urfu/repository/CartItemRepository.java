package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urfu.entity.Cart;
import ru.urfu.entity.CartItem;
import ru.urfu.entity.Goods;
import ru.urfu.entity.User;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteById(Long id);
    boolean existsByGoodsId(Long goodsId);
    Optional<CartItem> findByCartAndGoods(Cart cart, Goods goods);

}
