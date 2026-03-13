package ru.urfu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.Goods;
import ru.urfu.entity.Review;
import ru.urfu.entity.User;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.ReviewRepository;
import ru.urfu.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             GoodsRepository goodsRepository,
                             UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Review> getUserReviews(User user, Integer ratingFilter) {
        if (ratingFilter != null) {
            return reviewRepository.findByAuthorAndRating(user, ratingFilter);
        }
        return reviewRepository.findByAuthor(user);
    }

    @Override
    public double getGoodsAverageRating(Goods goods) {
        Double avg = reviewRepository.getAverageRating(goods);
        return avg != null ? avg : 0.0;
    }

    @Override
    public long getGoodsReviewCount(Goods goods) {
        return reviewRepository.countByGoods(goods);
    }

    @Override
    public double getSellerAverageRating(User seller) {
        List<Goods> sellerGoods = goodsRepository.findByUserEmail(seller.getEmail());
        List<Review> allReviews = reviewRepository.findByGoodsIn(sellerGoods);

        if (allReviews.isEmpty()) return 0.0;
        return allReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public void createReview(User author, Goods goods, int rating,
                             String comment, MultipartFile image) throws IOException {
        Review review = new Review();
        review.setAuthor(author);
        review.setGoods(goods);
        review.setRating(rating);
        review.setComment(comment);

        if (image != null && !image.isEmpty()) {
            String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path uploadDir = Paths.get("./uploads/reviews/");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(filename);
            image.transferTo(filePath.toFile());
            review.setImagePath("reviews/" + filename);
        }

        reviewRepository.save(review);

        // Обновляем средний рейтинг товара в базе
        updateGoodsRating(goods);
    }

    @Override
    public void deleteReview(Long id, User user) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Отзыв не найден"));

        // Проверка прав - только автор может удалить
        if (!review.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Вы не можете удалить этот отзыв");
        }

        Goods goods = review.getGoods();
        reviewRepository.delete(review);

        // Пересчитываем рейтинг после удаления
        updateGoodsRating(goods);
    }

    @Override
    public boolean hasReviewed(User user, Goods goods) {
        return reviewRepository.existsByAuthorAndGoods(user, goods);
    }

    @Override
    public List<Review> getGoodsReviews(Goods goods) {
        return reviewRepository.findByGoodsOrderByCreatedAtDesc(goods);
    }

    private void updateGoodsRating(Goods goods) {
        double avgRating = getGoodsAverageRating(goods);
        long reviewCount = getGoodsReviewCount(goods);

        // Обновляем поля в Goods entity
        goods.setAverageRating(avgRating);
        goods.setReviewCount(reviewCount);
        goodsRepository.save(goods);
    }
}