package ru.urfu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsQuestion;
import ru.urfu.entity.GoodsQuestionStatus;


import java.util.List;

@Repository
public interface GoodsQuestionRepository extends JpaRepository<GoodsQuestion, Long> {
    List<GoodsQuestion> findByStatus(GoodsQuestionStatus status);
    List<GoodsQuestion> findByGoods(Goods goods);
    List<GoodsQuestion> findByGoodsOrderByCreatedAtDesc(Goods goods);
}