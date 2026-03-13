package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.urfu.entity.User;
import ru.urfu.repository.MessageRepository;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.CartService;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CartService cartService;

    /**
     * Этот метод будет запускаться перед каждым вызовом любого контроллера.
     * Значения, добавленные в модель здесь, будут доступны во всех шаблонах.
     */
    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername());

            if (user != null) {
                // Считаем непрочитанные сообщения
                long unreadCount = messageRepository.countByReceiverAndIsReadFalse(user);
                model.addAttribute("unreadMessagesCount", unreadCount);

                // Считаем товары в корзине
                int cartCount = cartService.getCartByUser(user).getItems().size();
                model.addAttribute("cartItemsCount", cartCount);

                // Можно также прокинуть самого юзера, если он нужен везде
                model.addAttribute("currentUser", user);
            }
        } else {
            // Значения для анонимных пользователей
            model.addAttribute("unreadMessagesCount", 0);
            model.addAttribute("cartItemsCount", 0);
        }
    }
}