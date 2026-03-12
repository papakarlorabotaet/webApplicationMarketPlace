package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.GoodsStatus;
import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.GoodsService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class ProfileController {


    private final GoodsService goodsService;
    private final UserRepository userRepository;

    public ProfileController(GoodsService goodsService, UserRepository userRepository) {
        this.goodsService = goodsService;
        this.userRepository = userRepository;
    }

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

    @Value("${upload.path}")
    private String uploadPath;


    @PostMapping("/profile/avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        User user = userRepository.findByEmail(userDetails.getUsername());

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadPath, filename);

        Files.copy(file.getInputStream(), path);

        user.setAvatarPath(filename);
        userRepository.save(user);

        return "redirect:/profile";
    }



}