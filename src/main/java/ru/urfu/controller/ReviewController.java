package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.urfu.entity.*;
import ru.urfu.repository.*;
import ru.urfu.service.GoodsQuestionService;
import ru.urfu.service.ReviewService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final GoodsQuestionService goodsQuestionService;

    public ReviewController(ReviewRepository reviewRepository,
                            OrderRepository orderRepository,
                            GoodsRepository goodsRepository,
                            UserRepository userRepository,
                            ReviewService reviewService, GoodsQuestionService goodsQuestionService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
        this.reviewService = reviewService;

        this.goodsQuestionService = goodsQuestionService;
    }

    @GetMapping("/goods/{goodsId}")
    public String viewProductReviews(@PathVariable Long goodsId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        // Получаем отзывы
        List<Review> reviews = reviewService.getGoodsReviews(goods);
        double averageRating = reviewService.getGoodsAverageRating(goods);
        long reviewCount = reviewService.getGoodsReviewCount(goods);

        // Получаем вопросы (одобренные + вопросы текущего пользователя)
        List<GoodsQuestion> allQuestions = goodsQuestionService.findByGoods(goods);
        List<GoodsQuestion> questions = new ArrayList<>();


        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername());
            for (GoodsQuestion q : allQuestions) {
                // Показываем одобренные ИЛИ вопросы текущего пользователя
                if (q.getStatus() == GoodsQuestionStatus.APPROVED ||
                        q.getAuthor().getId().equals(user.getId())) {
                    questions.add(q);
                }
            }
        } else {
            // Аноним видит только одобренные
            for (GoodsQuestion q : allQuestions) {
                if (q.getStatus() == GoodsQuestionStatus.APPROVED) {
                    questions.add(q);
                }
            }
        }

        // Может ли пользователь оставить отзыв (купил ли товар)
        boolean canLeaveReview = false;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername());
               canLeaveReview = orderRepository.existsByBuyerAndGoods(user, goods)
                    && !reviewService.hasReviewed(user, goods);
        }



        model.addAttribute("goods", goods);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("questions", questions);
        model.addAttribute("canLeaveReview", canLeaveReview);

        return "productDetail";
    }

    @GetMapping("/myReviews")
    public String myReviews(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        List<Review> reviews = reviewRepository.findByAuthor(user);
        model.addAttribute("reviews", reviews);
        return "myReviews";
    }

    @PostMapping("/add")
    public String addReview(@RequestParam Long goodsId,
                            @RequestParam int rating,
                            @RequestParam String comment,
                            @RequestParam(value = "image", required = false) MultipartFile image,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername());
            Goods goods = goodsRepository.findById(goodsId)
                    .orElseThrow(() -> new RuntimeException("Товар не найден"));

            // Проверка: пользователь купил этот товар
            if (!orderRepository.existsByBuyerAndGoods(user, goods)) {
                redirectAttributes.addFlashAttribute("error", "Вы не покупали этот товар");
                return "redirect:/reviews/goods/" + goodsId;
            }

            // Проверка: ещё не оставлял отзыв
            if (reviewService.hasReviewed(user, goods)) {
                redirectAttributes.addFlashAttribute("error", "Вы уже оставляли отзыв на этот товар");
                return "redirect:/reviews/goods/" + goodsId;
            }

            reviewService.createReview(user, goods, rating, comment, image);
            redirectAttributes.addFlashAttribute("success", "Отзыв успешно добавлен");

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при загрузке фото");
        }

        return "redirect:/reviews/goods/" + goodsId;
    }

    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername());
            reviewService.deleteReview(id, user);
            redirectAttributes.addFlashAttribute("success", "Отзыв удалён");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/reviews/myReviews";
    }

    // ✅ Добавить метод для вопросов
    @PostMapping("/goods/{goodsId}/questions")
    public String addQuestion(@PathVariable Long goodsId,
                              @RequestParam String text,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {  // ✅ Добавляем параметр
        try {
            User author = userRepository.findByEmail(userDetails.getUsername());
            Goods goods = goodsRepository.findById(goodsId).orElseThrow();

            GoodsQuestion question = new GoodsQuestion();
            question.setGoods(goods);
            question.setAuthor(author);
            question.setText(text);
            question.setStatus(GoodsQuestionStatus.PENDING);

            goodsQuestionService.saveQuestion(question);

            //Используем flash-атрибут вместо параметра URL
            redirectAttributes.addFlashAttribute("success", "Вопрос отправлен на модерацию");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при отправке вопроса");
        }

        return "redirect:/reviews/goods/" + goodsId;  // ✅ Без параметров в URL
    }

}