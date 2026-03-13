package ru.urfu.service;

import org.springframework.web.multipart.MultipartFile;
import ru.urfu.entity.Goods;
import ru.urfu.entity.Review;
import ru.urfu.entity.User;

import java.io.IOException;
import java.util.List;

public interface ReviewService {

    public List<Review> getUserReviews(User user, Integer ratingFilter);
    public long getGoodsReviewCount(Goods goods);

    double getGoodsAverageRating(Goods goods);

    double getSellerAverageRating(User seller);

    void createReview(User user, Goods goods, int rating, String comment, MultipartFile image) throws IOException;

    void deleteReview(Long id, User user);

    boolean hasReviewed(User user, Goods goods);
    public List<Review> getGoodsReviews(Goods goods);
}
