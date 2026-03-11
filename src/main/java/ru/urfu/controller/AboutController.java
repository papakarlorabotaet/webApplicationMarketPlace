package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class AboutController {


    @GetMapping("/about")
    public String about(@AuthenticationPrincipal UserDetails userDetails){
        return "about";
    }
}
