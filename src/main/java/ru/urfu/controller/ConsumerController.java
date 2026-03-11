package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;

@Controller
@RequestMapping("/consumer")
public class ConsumerController {

    private final UserRepository userRepository;

    public ConsumerController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("cartItemsCount", 0); // позже
        return "consumer/profileConsumer"; // или "consumer/accountConsumer", если файл так называется
    }
}