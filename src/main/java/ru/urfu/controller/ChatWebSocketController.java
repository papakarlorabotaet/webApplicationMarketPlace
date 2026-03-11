package ru.urfu.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.urfu.dto.MessageDto;
import ru.urfu.entity.Message;
import ru.urfu.entity.User;
import ru.urfu.repository.MessageRepository;
import ru.urfu.repository.UserRepository;

import java.time.LocalDateTime;
import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate,
                                   MessageRepository messageRepository,
                                   UserRepository userRepository) {

        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    // Когда клиент отправляет сообщение на /app/chat.sendMessage
    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload MessageDto messageDto, Principal principal) {
        User sender = userRepository.findByEmail(principal.getName());
        User receiver = userRepository.findById(messageDto.getReceiverId()).orElseThrow();

        // 1. Сохраняем в базу, чтобы история осталась
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageDto.getContent());
        message.setTimestamp(LocalDateTime.now());
        messageRepository.save(message);

        // 2. Отправляем мгновенно получателю в его персональную очередь
        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(), "/queue/messages",
                message
        );
    }
}
