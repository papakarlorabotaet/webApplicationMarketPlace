package ru.urfu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsQuestion;
import ru.urfu.entity.GoodsQuestionStatus;


import ru.urfu.repository.GoodsQuestionRepository;


import java.util.List;

@Service
@Transactional
public class GoodsQuestionServiceImpl implements GoodsQuestionService {

    private final GoodsQuestionRepository goodsQuestionRepository;

    public GoodsQuestionServiceImpl(GoodsQuestionRepository goodsQuestionRepository) {
        this.goodsQuestionRepository = goodsQuestionRepository;
    }

    @Override
    public List<GoodsQuestion> findAllPendingQuestions() {
        // Возвращаем все вопросы, ожидающие проверки
        return goodsQuestionRepository.findByStatus(GoodsQuestionStatus.PENDING);
    }

    @Override
    public void updateStatus(Long questionId, GoodsQuestionStatus status) {
        GoodsQuestion goodsQuestion = goodsQuestionRepository.findById(questionId).get();
        goodsQuestion.setStatus(status);
        goodsQuestionRepository.save(goodsQuestion);
    }

    @Override
    public void saveQuestion(GoodsQuestion goodsQuestion) {
        goodsQuestionRepository.save(goodsQuestion);
    }

    @Override
    public GoodsQuestion findById(Long questionId) {
        return goodsQuestionRepository.findById(questionId).orElse(null);
    }

    @Override
    public List<GoodsQuestion> findAllByGoods(Goods goods) {
        return goodsQuestionRepository.findByGoods(goods);
    }

    @Override
    public List<GoodsQuestion> findByGoods(Goods goods) {
        return goodsQuestionRepository.findByGoodsOrderByCreatedAtDesc(goods);
    }
}