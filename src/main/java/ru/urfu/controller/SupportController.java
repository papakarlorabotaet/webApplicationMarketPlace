package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.urfu.entity.*;


import ru.urfu.repository.TransactionRepository;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.GoodsQuestionService;
import ru.urfu.service.GoodsService;
import ru.urfu.service.PaymentService;
import ru.urfu.service.UserService;

import java.util.List;

@Controller
@RequestMapping("/support")
public class SupportController {


    private final UserService userService;
    private final UserRepository userRepository;
    private final GoodsService goodsService;
    private final GoodsQuestionService goodsQuestionService;
    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;


    @Autowired
    public SupportController(UserRepository userRepository,
                             UserService userService,
                             GoodsService goodsService,
                             GoodsQuestionService goodsQuestionService,
                             PaymentService paymentService, TransactionRepository transactionRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.goodsService = goodsService;
        this.goodsQuestionService = goodsQuestionService;
        this.paymentService = paymentService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/users")
    public String listUsers(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        // Получаем всех пользователей
        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);

        // Передаем текущего админа для отображения в шапке
        User currentUser = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", currentUser);

        return "support/users";
    }

    @GetMapping("/deleteUser")
    public String deleteSeller(@RequestParam String userEmail,
                               @AuthenticationPrincipal UserDetails userDetails) {
        User userDelete = userRepository.findByEmail(userEmail); //полчаем инфу

        userService.deleteUserById(userDelete.getId()); //удаляем пользователя из базы
        return "redirect:/support/users";
    }


//    @PostMapping("/goods/approve/{id}")
//    public String approveGoods(@PathVariable Long id) {
//        goodsService.updateStatus(id, GoodsStatus.APPROVED);
//        return "redirect:/support/orders";
//    }

    @GetMapping("/profile")
    public String supportProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        long pendingCount = goodsService.findAllPendingGoods().size();// Добавляем счетчик для карточки статистики
        long pendingQuestionsCount = goodsQuestionService.findAllPendingQuestions().size();//добавляем счетчик для вопросов
        model.addAttribute("pendingGoodsCount", pendingCount);
        model.addAttribute("pendingQuestionsCount", pendingQuestionsCount);
        return "support/profileSupport";
    }


    // Панель АДМИНА: Модерация
    @GetMapping("/orders")
    public String supportModeration(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("pendingGoods", goodsService.findAllPendingGoods());
        model.addAttribute("pendingGoodsCount", goodsService.findAllPendingGoods().size());
        return "support/orders";
    }

    // Действие АДМИНА: Одобрение

    @PostMapping("/orders/approve/{id}")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername());
        goodsService.updateStatus(id, GoodsStatus.APPROVED, admin);
        return "redirect:/support/orders";
    }


    // Действие АДМИНА: Отклонение
    @PostMapping("/orders/reject/{id}")
    public String reject(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername());
        goodsService.updateStatus(id, GoodsStatus.REJECTED, admin);
        return "redirect:/support/orders";
    }

    // === БЛОК МОДЕРАЦИИ ВОПРОСОВ К ТОВАРАМ ===

    // Страница со списком вопросов, ожидающих модерации
    @GetMapping("/questions")
    public String supportQuestionsModeration(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);


        model.addAttribute("pendingQuestions", goodsQuestionService.findAllPendingQuestions());

        return "/support/questions";
    }

    // Одобрить вопрос (после этого он появится в карточке товара)
    @PostMapping("/questions/approve/{id}")
    public String approveQuestion(@PathVariable Long id) {
        goodsQuestionService.updateStatus(id, GoodsQuestionStatus.APPROVED);
        return "redirect:/support/questions";
    }

    // Отклонить/удалить вопрос
    @PostMapping("/questions/reject/{id}")
    public String rejectQuestion(@PathVariable Long id) {
        goodsQuestionService.updateStatus(id, GoodsQuestionStatus.REJECTED);
        return "redirect:/support/questions";
    }


    @GetMapping("/payments/user/{email}")
    public String getUserPayments(@PathVariable String email, Model model) {
        // Получаем платежи только для этого пользователя
        List<Transaction> userTransactions = transactionRepository.findByUserAndStatus(userRepository.findByEmail(email), TransactionStatusEnum.PENDING);

        model.addAttribute("pendingDeposits", userTransactions);
        model.addAttribute("targetUser", email); // Чтобы отобразить в заголовке, чьи это платежи
        return "/support/payments"; // Используем ваш payments.html
    }

    @PostMapping("/payments/approve/{id}")
    public String approveDeposit(@PathVariable Long id) {
        paymentService.approveDeposit(id);
        Transaction tx = transactionRepository.findById(id).orElse(null);
        if (tx != null && tx.getUser() != null) {
            return "redirect:/support/payments/user/" + tx.getUser().getEmail();
        }
        return "redirect:/support/users"; // Фолбэк, если что-то пошло не так
    }

    @PostMapping("/payments/reject/{id}")
    public String rejectDeposit(@PathVariable Long id) {
        paymentService.rejectDeposit(id);
        Transaction tx = transactionRepository.findById(id).orElse(null);
        if (tx != null && tx.getUser() != null) {
            return "redirect:/support/payments/user/" + tx.getUser().getEmail();
        }
        return "redirect:/support/users";
    }


}
