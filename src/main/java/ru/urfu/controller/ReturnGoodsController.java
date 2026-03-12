package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import ru.urfu.entity.Order;
import ru.urfu.entity.User;
import ru.urfu.repository.OrderRepository;
import ru.urfu.repository.UserRepository;

@Controller
public class ReturnGoodsController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;


    public ReturnGoodsController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // Возврат в один клик
    @PostMapping("/return/{orderId}")
    public String returnOrder(@PathVariable Long orderId, @AuthenticationPrincipal UserDetails userDetails) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User user = userRepository.findByEmail(userDetails.getUsername());

        // Проверяем, что заказ принадлежит юзеру и статус позволяет возврат
        if (order.getBuyer().getId().equals(user.getId()) && "Доставлен".equals(order.getStatus())) {
            order.setStatus("Возврат оформлен");
            user.setAccountBalance(user.getAccountBalance().add(order.getTotalPrice())); // Возвращаем деньги
            userRepository.save(user);
            orderRepository.save(order);
        }
        return "redirect:/consumer/orders";
    }

    // Заглушка безопасной онлайн-оплаты
    @PostMapping("/pay/{orderId}")
    public String payOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus("Оплачен");
        orderRepository.save(order);
        return "redirect:/consumer/orders";
    }
}
