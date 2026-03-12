package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/settings")
public class SettingsController {
    private final UserRepository userRepository;
    private final  PasswordEncoder passwordEncoder;

    public SettingsController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${upload.path}")
    private String uploadPath;

    @GetMapping
    public String settings(Model model,
                           @AuthenticationPrincipal UserDetails userDetails){

        User user = userRepository.findByEmail(userDetails.getUsername());

        model.addAttribute("user", user);

        return "profile/settings";
    }

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam MultipartFile avatar,
                               @AuthenticationPrincipal UserDetails userDetails)
            throws IOException {

        User user = userRepository.findByEmail(userDetails.getUsername());

        String filename = UUID.randomUUID() + "_" + avatar.getOriginalFilename();

        Path path = Paths.get(uploadPath, filename);

        Files.copy(avatar.getInputStream(), path);

        user.setAvatarPath(filename);

        userRepository.save(user);

        return "redirect:/settings";
    }

    @PostMapping("/email")
    public String changeEmail(@RequestParam String email,
                              @AuthenticationPrincipal UserDetails userDetails){

        User user = userRepository.findByEmail(userDetails.getUsername());

        user.setEmail(email);

        userRepository.save(user);

        return "redirect:/settings";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @AuthenticationPrincipal UserDetails userDetails){

        User user = userRepository.findByEmail(userDetails.getUsername());

        if(passwordEncoder.matches(oldPassword, user.getPassword())){

            user.setPassword(passwordEncoder.encode(newPassword));

            userRepository.save(user);
        }

        return "redirect:/settings";
    }

}