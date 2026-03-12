package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.urfu.entity.Cart;
import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.CartService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        User user = userRepository.findByEmail(userDetails.getUsername());

        Cart cart = cartService.getCartByUser(user);

        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", cartService.calculateTotal(cart));

        return "cart";
    }


    @PostMapping("/add")
    public String addToCart(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam Long goodsId,
                            @RequestParam(defaultValue = "1") int quantity) {

        User user = userRepository.findByEmail(userDetails.getUsername());

        cartService.addToCart(user, goodsId, quantity);

        return "redirect:/cart";
    }


    @PostMapping("/remove")
    public String removeFromCart(@RequestParam Long itemId) {

        cartService.removeFromCart(itemId);

        return "redirect:/cart";
    }

}