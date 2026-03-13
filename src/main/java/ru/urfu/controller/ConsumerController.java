package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.urfu.entity.Order;
import ru.urfu.entity.Transaction;
import ru.urfu.entity.TransactionType;
import ru.urfu.entity.User;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.OrderService;
import ru.urfu.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/consumer")
public class ConsumerController {

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;

    public ConsumerController(UserRepository userRepository, OrderService orderService, PaymentService paymentService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        List<Order> recentOrders = orderService.findOrdersBySeller(user);
        model.addAttribute("user", user);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("cartItemsCount", 0); // позже
        return "consumer/profileConsumer";
    }

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("orders", orderService.findOrdersByBuyer(user));
        return "consumer/orders"; // Возвращаем наш новый шаблон
    }

    @GetMapping("/payments/deposit")
    public String depositPage() {
        return "/consumer/deposit";
    }

    @PostMapping("/payments/deposit")
    public String createDeposit(@RequestParam BigDecimal amount,
                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        paymentService.createDepositRequest(user, amount);
        return "redirect:/consumer/payments?success";
    }

    @GetMapping("/payments")
    public String paymentsPage(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(required = false) TransactionType type,
                               @RequestParam(required = false) LocalDateTime dateFrom,
                               @RequestParam(required = false) LocalDateTime dateTo,
                               Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());

        // Текущий баланс
        model.addAttribute("currentBalance", user.getAccountBalance());

        // История транзакций с фильтрами
        List<Transaction> transactions = paymentService
                .getUserTransactions(user, type, dateFrom, dateTo);
        model.addAttribute("transactions", transactions);

        // Статистика
        model.addAttribute("totalIncome", paymentService.calculateTotalIncome(user));
        model.addAttribute("totalExpense", paymentService.calculateTotalExpense(user));

        return "consumer/payments";
    }


}