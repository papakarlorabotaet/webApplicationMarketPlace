package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsStatus;
import ru.urfu.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {
    Goods findByNameAndUser(String name, User user);

    Goods findByIdAndUser(Long id, User user);

    Optional<Goods> findById(Long id);

    List<Goods> findByCategoryId(Long categoryId);

    List<Goods> findByModerationStatus(GoodsStatus status);


    List<Goods> findByUserEmail(String email);   // если у товара есть связь с User через поле user

    List<Goods> findByNameContainingIgnoreCase(String name);


    @Query("SELECT g FROM Goods g WHERE " +
            "(:categoryId IS NULL OR g.category.id = :categoryId) AND " +
            "(LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "g.price >= :minPrice AND g.price <= :maxPrice AND " +
            "g.moderationStatus = :status AND " + // Всегда проверяем статус
            "(:sellerEmail IS NULL OR g.user.email = :sellerEmail)") // Фильтр по продавцу (если передан)

    java.util.List<ru.urfu.entity.Goods> findFilteredGoods(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("status") GoodsStatus status,
            @Param("sellerEmail") String sellerEmail);

    List<Goods> findByNameContainingIgnoreCaseAndPriceBetweenAndModerationStatus(
            String name,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            GoodsStatus status);

    List<Goods> findByPriceBetweenAndModerationStatus(int minPrice, int maxPrice, GoodsStatus status);


}
