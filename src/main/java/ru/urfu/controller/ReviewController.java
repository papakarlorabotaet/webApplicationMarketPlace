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
import ru.urfu.service.ReviewService;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final ReviewService reviewService;

    public ReviewController(ReviewRepository reviewRepository,
                            OrderRepository orderRepository,
                            GoodsRepository goodsRepository,
                            UserRepository userRepository,
                            ReviewService reviewService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
        this.reviewService = reviewService;
    }

    @GetMapping("/myReviews")
    public String myReviews(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        List<Review> reviews = reviewRepository.findByAuthor(user);
        model.addAttribute("reviews", reviews);
        return "myReviews";
    }

    // Страница товара с отзывами
    @GetMapping("/goods/{goodsId}")
    public String viewProductReviews(@PathVariable Long goodsId, Model model) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        List<Review> reviews = reviewService.getGoodsReviews(goods);
        double averageRating = reviewService.getGoodsAverageRating(goods);
        long reviewCount = reviewService.getGoodsReviewCount(goods);

        model.addAttribute("goods", goods);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviewCount);

        return "productDetail";
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
}