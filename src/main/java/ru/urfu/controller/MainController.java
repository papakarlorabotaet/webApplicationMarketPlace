package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.urfu.service.GoodsService;

@Controller
public class MainController {

    private final GoodsService goodsService;

    @Autowired
    public MainController(GoodsService goodsService) {
        this.goodsService = goodsService;

    }

    @GetMapping("/list")
    public String listApprovedGoods(Model model) {
        // Передаем в модель только одобренные товары
        model.addAttribute("goods", goodsService.findAllApprovedGoods());
        return "list"; // название вашего HTML файла без .html
    }
}