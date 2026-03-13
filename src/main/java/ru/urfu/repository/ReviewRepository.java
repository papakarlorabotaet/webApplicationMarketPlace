package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.urfu.entity.Goods;
import ru.urfu.entity.Review;
import ru.urfu.entity.User;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByAuthor(User author);

    List<Review> findByAuthorAndRating(User author, Integer rating);

    List<Review> findByGoods(Goods goods);

    @Query("SELECT r FROM Review r WHERE r.goods = :goods ORDER BY r.createdAt DESC")
    List<Review> findByGoodsOrderByCreatedAtDesc(@Param("goods") Goods goods);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.goods = :goods")
    Double getAverageRating(@Param("goods") Goods goods);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.goods = :goods")
    Long countByGoods(@Param("goods") Goods goods);

    List<Review> findByGoodsIn(List<Goods> goods);

    boolean existsByAuthorAndGoods(User author, Goods goods);
}

