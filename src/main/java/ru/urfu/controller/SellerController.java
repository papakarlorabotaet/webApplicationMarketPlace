package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.OrderService;

@Controller
@RequestMapping("/seller")
public class SellerController {

    private final UserRepository userRepository;
    private final OrderService orderService;

    public SellerController(UserRepository userRepository, OrderService orderService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User seller = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", seller);

        // Добавляем заказы продавца в модель
        model.addAttribute("orders", orderService.findOrdersBySeller(seller));

        // Здесь можно добавить другую статистику (количество товаров, выручку и т.д.)
        return "seller/profileSeller";
    }

    @PostMapping("/import-orders")
    public String importOrders(@RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal UserDetails userDetails) {
        orderService.importOrders(file, userDetails.getUsername());
        return "redirect:/seller/profileSeller";
    }
}