package ru.urfu.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import ru.urfu.dto.UserDto;


import ru.urfu.entity.User;
import ru.urfu.service.UserService;


import javax.validation.Valid;


@Controller
public class SecurityController {

    private final UserService userService;

    public SecurityController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/index")
    public String home() {
        return "index";
    }

    @GetMapping("/permissionDenied")
    public String permissionDenied() {
        return "permission-denied";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register/consumer")
    public String showRegistrationFormConsumer(Model model) {
        UserDto user = new UserDto();
        user.setRole("ROLE_CONSUMER");
        model.addAttribute("user", user);
        return "consumer/registerConsumer";
    }

    @GetMapping("/register/seller")
    public String showRegistrationFormSeller(Model model) {
        UserDto user = new UserDto();
        user.setRole("ROLE_SELLER");
        model.addAttribute("user", user);
        return "seller/registerSeller";
    }


    @PostMapping("/register/save")
    public String registration(@Valid @ModelAttribute("user") UserDto userDto,
                               BindingResult result,
                               Model model) {
        User existingUser = userService.findUserByEmail(userDto.getEmail());

        if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
            result.rejectValue("email", null, "Этот email уже занят");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", userDto);
            if ("ROLE_SELLER".equals(userDto.getRole())) {// Используем правильный путь в зависимости от роли
                return "seller/registerSeller";
            } else {
                return "consumer/registerConsumer";
            }
        }

        userService.saveUser(userDto);
        return "redirect:/login?success";
    }
}
