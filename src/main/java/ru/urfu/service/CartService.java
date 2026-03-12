package ru.urfu.service;

import org.springframework.stereotype.Service;
import ru.urfu.entity.Cart;
import ru.urfu.entity.CartItem;
import ru.urfu.entity.Goods;
import ru.urfu.entity.User;
import ru.urfu.repository.CartItemRepository;
import ru.urfu.repository.CartRepository;
import ru.urfu.repository.GoodsRepository;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final GoodsRepository goodsRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, GoodsRepository goodsRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.goodsRepository = goodsRepository;
    }

    public Cart getCartByUser(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    @Transactional
    public void addToCart(User user, Long goodsId, int quantity) {

        Cart cart = getCartByUser(user);

        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartAndGoods(cart, goods);

        if (existingItem.isPresent()) {

            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);

            cartItemRepository.save(item);

        } else {

            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setGoods(goods);
            newItem.setQuantity(quantity);

            cartItemRepository.save(newItem);
        }
    }

    @Transactional
    public void removeFromCart(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }

    @Transactional
    public BigDecimal calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getGoods().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}