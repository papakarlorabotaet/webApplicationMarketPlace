package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.OrderService;

@Controller
@RequestMapping("/consumer")
public class ConsumerController {

    private final UserRepository userRepository;
    private final OrderService orderService;

    public ConsumerController(UserRepository userRepository, OrderService orderService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("cartItemsCount", 0); // позже
        return "consumer/profileConsumer";
    }

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("orders", orderService.findOrdersBySeller(user));
        return "consumer/orders"; // Возвращаем наш новый шаблон
    }


}