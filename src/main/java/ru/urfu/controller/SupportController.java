package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.urfu.entity.GoodsStatus;

import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.GoodsService;
import ru.urfu.service.UserService;

@Controller
public class SupportController {


    private final UserService userService;
    private final UserRepository userRepository;
    private final GoodsService goodsService;

    @Autowired
    public SupportController(UserRepository userRepository,
                             UserService userService,
                             GoodsService goodsService){
        this.userService = userService;
        this.userRepository = userRepository;
        this.goodsService = goodsService;
    }

    @GetMapping("/deleteUser")
    public String deleteSeller(@RequestParam String userEmail,
                             @AuthenticationPrincipal UserDetails userDetails){
         User userDelete = userRepository.findByEmail(userEmail); //полчаем инфу

        userService.deleteUserById(userDelete.getId()); //удаляем пользователя из базы
        return "redirect:/users";
    }


    @PostMapping("/goods/approve/{id}")
    public String approveGoods(@PathVariable Long id) {
        goodsService.updateStatus(id, GoodsStatus.APPROVED);
        return "redirect:/support/orders";
    }

    @GetMapping("/support/profileSupport")
    public String supportProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        return "support/profileSupport";
    }


    // Панель АДМИНА: Модерация
    @GetMapping("/support/orders")
    public String supportModeration(Model model) {
        model.addAttribute("pendingGoods", goodsService.findAllPendingGoods());
        return "support/orders";
    }

    // Действие АДМИНА: Одобрение
    @PostMapping("/support/approve/{id}")
    public String approve(@PathVariable Long id) {
        goodsService.updateStatus(id, GoodsStatus.APPROVED);
        return "redirect:/support/orders";
    }

    // Действие АДМИНА: Отклонение
    @PostMapping("/support/reject/{id}")
    public String reject(@PathVariable Long id) {
        goodsService.updateStatus(id, GoodsStatus.REJECTED);
        return "redirect:/support/orders";
    }
}
