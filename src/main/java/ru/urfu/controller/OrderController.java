package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.urfu.entity.Goods;
import ru.urfu.entity.Order;
import ru.urfu.entity.OrderItem;
import ru.urfu.entity.User;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.MessageRepository;
import ru.urfu.repository.OrderRepository;
import ru.urfu.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderController(GoodsRepository goodsRepository, UserRepository userRepository, OrderRepository orderRepository) {
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }


    @PostMapping("/buy-now")
    @Transactional
    public String buyNow(@RequestParam Long goodsId,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        User consumer = userRepository.findByEmail(userDetails.getUsername());
        Goods good = goodsRepository.findById(goodsId).orElseThrow();

        // 1. Проверка наличия товара
        if (good.getQuantity() <= 0) {
            return "redirect:/list?error=no_stock";
        }

        // 2. Проверка баланса
        BigDecimal consumerBalance = consumer.getAccountBalance();
        if (consumerBalance == null) {
            consumerBalance = BigDecimal.ZERO;
        }
        // 2. Проверка достаточности средств
        if (consumerBalance.compareTo(good.getPrice()) < 0) {
            return "redirect:/list?error=no_money";
        }

        // 3. Финансовые операции
        consumer.setAccountBalance(consumer.getAccountBalance().subtract(good.getPrice()));

        User seller = good.getUser(); // В вашей сущности это может быть getSeller() или getUser()
        BigDecimal sellerBalance = seller.getAccountBalance() != null ? seller.getAccountBalance() : BigDecimal.ZERO;
        seller.setAccountBalance(sellerBalance.add(good.getPrice()));

        // 4. Уменьшение остатка товара
        good.setQuantity(good.getQuantity() - 1);

        // 5. Создание заказа
        Order order = new Order();
        order.setBuyer(consumer);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalPrice(good.getPrice());
        order.setStatus("Оплачен");

        // Добавление товара в заказ (OrderItem)
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setGoods(good);
        item.setQuantity(1);
        order.setItems(Collections.singletonList(item));

        orderRepository.save(order);
        userRepository.save(consumer);
        userRepository.save(seller);
        goodsRepository.save(good);

        return "redirect:/receipt/" + order.getId();
    }

    @GetMapping("/receipt/{id}")
    public String showReceipt(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        return "order/receipt";
    }

}


