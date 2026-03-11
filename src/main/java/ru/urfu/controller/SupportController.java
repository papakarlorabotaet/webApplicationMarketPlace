package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.urfu.entity.*;

import ru.urfu.entity.GoodsQuestionStatus;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.GoodsQuestionService;
import ru.urfu.service.GoodsService;
import ru.urfu.service.UserService;

import java.util.List;

@Controller
public class SupportController {


    private final UserService userService;
    private final UserRepository userRepository;
    private final GoodsService goodsService;
    private final GoodsQuestionService goodsQuestionService;

    @Autowired
    public SupportController(UserRepository userRepository,
                             UserService userService,
                             GoodsService goodsService, GoodsQuestionService goodsQuestionService){
        this.userService = userService;
        this.userRepository = userRepository;
        this.goodsService = goodsService;
        this.goodsQuestionService = goodsQuestionService;
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
                             @AuthenticationPrincipal UserDetails userDetails){
         User userDelete = userRepository.findByEmail(userEmail); //полчаем инфу

        userService.deleteUserById(userDelete.getId()); //удаляем пользователя из базы
        return "redirect:/users";
    }


//    @PostMapping("/goods/approve/{id}")
//    public String approveGoods(@PathVariable Long id) {
//        goodsService.updateStatus(id, GoodsStatus.APPROVED);
//        return "redirect:/support/orders";
//    }

    @GetMapping("/support/profile")
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
    @GetMapping("/support/orders")
    public String supportModeration(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("pendingGoods", goodsService.findAllPendingGoods());
        model.addAttribute("pendingGoodsCount", goodsService.findAllPendingGoods().size());
        return "support/orders";
    }

    // Действие АДМИНА: Одобрение

    @PostMapping("/support/approve/{id}")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername());
        goodsService.updateStatus(id, GoodsStatus.APPROVED, admin);
        return "redirect:/support/orders";
    }


    // Действие АДМИНА: Отклонение
    @PostMapping("/support/reject/{id}")
    public String reject(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername());
        goodsService.updateStatus(id, GoodsStatus.REJECTED, admin);
        return "redirect:/support/orders";
    }

    // === БЛОК МОДЕРАЦИИ ВОПРОСОВ К ТОВАРАМ ===

    // Страница со списком вопросов, ожидающих модерации
    @GetMapping("/support/questions")
    public String supportQuestionsModeration(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);


        model.addAttribute("pendingQuestions", goodsQuestionService.findAllPendingQuestions());

        return "support/questions"; // Нужно будет создать HTML шаблон support/questions.html
    }

    // Одобрить вопрос (после этого он появится в карточке товара)
    @PostMapping("/support/questions/approve/{id}")
    public String approveQuestion(@PathVariable Long id) {
         goodsQuestionService.updateStatus(id, GoodsQuestionStatus.APPROVED);
        return "redirect:/support/questions";
    }

    // Отклонить/удалить вопрос
    @PostMapping("/support/questions/reject/{id}")
    public String rejectQuestion(@PathVariable Long id) {
         goodsQuestionService.updateStatus(id, GoodsQuestionStatus.REJECTED);
        return "redirect:/support/questions";
    }

}
