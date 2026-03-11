package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsStatus;
import ru.urfu.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {
    Goods findByNameAndUser(String name, User user);

    Optional<Goods> findById(Long id);

    List<Goods> findByModerationStatus(GoodsStatus status);

    List<Goods> findByUserEmail(String email);   // если у товара есть связь с User через поле user

    List<Goods> findByNameContainingIgnoreCase(String name);


}
