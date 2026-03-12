package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.*;
import ru.urfu.repository.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository; // Нужно будет создать интерфейс ReviewRepository
    private final OrderRepository orderRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;

    public ReviewController(ReviewRepository reviewRepository, OrderRepository orderRepository, GoodsRepository goodsRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public String addReview(@RequestParam Long goodsId, @RequestParam int rating,
                            @RequestParam String comment, @RequestParam("image") MultipartFile image,
                            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User user = userRepository.findByEmail(userDetails.getUsername());
        Goods goods = goodsRepository.findById(goodsId).orElseThrow();

        // Дипломная фича: Проверяем, что пользователь реально купил этот товар
        if (!orderRepository.existsByBuyerAndGoods(user, goods)) {
            return "redirect:/list?error=not_bought";
        }

        Review review = new Review();
        review.setAuthor(user);
        review.setGoods(goods);
        review.setRating(rating);
        review.setComment(comment);

        if (!image.isEmpty()) {
            String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get("./uploads/images/").toAbsolutePath().resolve(filename);
            Files.createDirectories(filePath.getParent());
            image.transferTo(filePath.toFile());
            review.setImagePath(filename);
        }

        reviewRepository.save(review);
        return "redirect:/list";
    }
}
