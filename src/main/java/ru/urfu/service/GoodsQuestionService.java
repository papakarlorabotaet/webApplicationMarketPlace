package ru.urfu.service;

import ru.urfu.entity.GoodsQuestion;
import ru.urfu.entity.GoodsQuestionStatus;
import java.util.List;

public interface GoodsQuestionService {
    List<GoodsQuestion> findAllPendingQuestions();
    void updateStatus(Long questionId, GoodsQuestionStatus status);
    void saveQuestion(GoodsQuestion question);
}