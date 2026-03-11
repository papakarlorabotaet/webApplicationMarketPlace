package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.urfu.entity.GoodsStatus;
import ru.urfu.service.GoodsService;

@Controller
public class ProfileController {

    @Autowired
    private GoodsService goodsService;

    @GetMapping("/profile")
    public String profileRedirect(@AuthenticationPrincipal UserDetails userDetails) {
        // Здесь можно определить роль и вернуть редирект
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"))) {
            return "redirect:/seller/profileSeller";
        } else if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPPORT"))) {
            return "redirect:/support/profileSupport";
        } else {
            return "redirect:/consumer/profileConsumer";
        }

    }



}