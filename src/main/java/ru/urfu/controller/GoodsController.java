package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.urfu.dto.GoodsDto;
import ru.urfu.service.GoodsService;

import javax.validation.Valid;
import java.util.List;

@Controller
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

//    @GetMapping("/list")
//    public String showListOfGoods(Model model, @AuthenticationPrincipal UserDetails userDetails) {
//
//        List<GoodsDto> goodsDtoList = goodsService.findAllGoods();
//
//
//        model.addAttribute("goods", goodsDtoList);
//
//        return "list";
//    }
//
//    @PostMapping("/list/save")
//    public String registration(@Valid @ModelAttribute("goods") GoodsDto goodsDto,
//                               @AuthenticationPrincipal UserDetails userDetails,
//                               BindingResult result,
//                               Model model) {
//
//        goodsService.saveGoods(goodsDto, userDetails.getUsername());
//
//        return "redirect:/seller/my-goods";
//    }


}
