package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.urfu.entity.Goods;
import ru.urfu.entity.GoodsQuestion;
import ru.urfu.entity.GoodsQuestionStatus;
import ru.urfu.entity.User;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.GoodsQuestionService;

@Controller
@RequestMapping("/questions")
public class QuestionController {
    private final GoodsQuestionService goodsQuestionService;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;

    public QuestionController(GoodsQuestionService goodsQuestionService, GoodsRepository goodsRepository, UserRepository userRepository) {
        this.goodsQuestionService = goodsQuestionService;
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public String addQuestion(@RequestParam Long goodsId,
                              @RequestParam String text,
                              @AuthenticationPrincipal UserDetails userDetails) {
        User author = userRepository.findByEmail(userDetails.getUsername());
        Goods goods = goodsRepository.findById(goodsId).orElseThrow();

        GoodsQuestion question = new GoodsQuestion();
        question.setGoods(goods);
        question.setAuthor(author);
        question.setText(text);
        question.setStatus(GoodsQuestionStatus.PENDING);

        goodsQuestionService.saveQuestion(question);
        return "redirect:/reviews/goods/" + goodsId;
    }
}