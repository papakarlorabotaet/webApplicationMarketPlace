package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.urfu.entity.Message;
import ru.urfu.entity.User;
import ru.urfu.repository.MessageRepository;
import ru.urfu.repository.UserRepository;

@Controller
@RequestMapping("/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageController(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    // Список диалогов пользователя
    @GetMapping
    public String myMessages(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("messages", messageRepository.findAllRecentDialogs(user.getId()));
        return "messages/list";
    }

    // Отправка сообщения (базовая версия через форму)
    @PostMapping("/send")
    public String sendMessage(@RequestParam Long receiverId,
                              @RequestParam String content,
                              @AuthenticationPrincipal UserDetails userDetails) {
        User sender = userRepository.findByEmail(userDetails.getUsername());
        User receiver = userRepository.findById(receiverId).orElseThrow();

        Message msg = new Message();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content);
        messageRepository.save(msg);

        return "redirect:/messages/chat/" + receiverId;
    }

    @GetMapping("/chat/{companionId}")
    public String openChat(@PathVariable Long companionId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername());
        User companion = userRepository.findById(companionId).orElseThrow();

        // 1. Помечаем входящие сообщения от этого собеседника как прочитанные
        messageRepository.markAsRead(companion, currentUser);

        // 2. Список всех диалогов (для левой панели)
        model.addAttribute("messages", messageRepository.findAllRecentDialogs(currentUser.getId()));

        // 3. История переписки именно с этим человеком (для правой панели)
        model.addAttribute("chatHistory", messageRepository.findChatHistory(currentUser, companion));
        model.addAttribute("companion", companion);

        return "messages/list";
    }


}