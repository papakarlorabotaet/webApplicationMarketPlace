package ru.urfu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.urfu.dto.GoodsDto;
import ru.urfu.entity.Goods;
import ru.urfu.entity.User;
import ru.urfu.repository.CategoryRepository;
import ru.urfu.repository.GoodsRepository;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.GoodsService;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class MainController {

    private final GoodsService goodsService;
    private final CategoryRepository categoryRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;

    @Autowired
    public MainController(GoodsService goodsService,
                          CategoryRepository categoryRepository,
                          GoodsRepository goodsRepository,
                          UserRepository userRepository) {
        this.goodsService = goodsService;
        this.categoryRepository = categoryRepository;
        this.goodsRepository = goodsRepository;
        this.userRepository = userRepository;
    }


    @GetMapping("/list")
    public String listApprovedGoods(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "0") BigDecimal minPrice,
            @RequestParam(required = false, defaultValue = "99999999") BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyMine, // Параметр для продавца
            @AuthenticationPrincipal UserDetails userDetails, // Получаем текущего юзера
            Model model) {

        String sellerEmailFilter = null;

        // Логика фильтра "Мои товары"
        if (onlyMine && userDetails != null) {
            // Проверяем, есть ли у пользователя роль SELLER
            boolean isSeller = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"));
            if (isSeller) {
                sellerEmailFilter = userDetails.getUsername(); // Будем искать только товары этого продавца
            }
        }
        int cartCount = 0;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername());
            // Если у вас уже есть CartService, можно вызвать его:
            // cartCount = cartService.getCartByUser(user).getItems().size();
        }

        model.addAttribute("categories", categoryRepository.findAll());

        // Передаем sellerEmailFilter в сервис
        List<GoodsDto> goods = goodsService.findFilteredGoods(categoryId, search, minPrice, maxPrice, sellerEmailFilter);

        model.addAttribute("goods", goods);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("search", search);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("onlyMine", onlyMine); // Чтобы галочка не слетала
        model.addAttribute("cartItemsCount", cartCount);

        return "list";
    }


}