//package ru.urfu.controller;
//
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Controller;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import ru.urfu.entity.Transaction;
//import ru.urfu.entity.TransactionStatusEnum;
//import ru.urfu.entity.TransactionType;
//import ru.urfu.entity.User;
//import ru.urfu.repository.TransactionRepository;
//import ru.urfu.repository.UserRepository;
//import ru.urfu.service.PaymentService;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Controller
//@RequestMapping("/consumer/payments")
//public class PaymentController {
//
//    private final PaymentService paymentService;
//    private final TransactionRepository transactionRepository;
//    private final UserRepository userRepository;
//
//    public PaymentController(PaymentService paymentService,
//                             TransactionRepository transactionRepository,
//                             UserRepository userRepository) {
//        this.paymentService = paymentService;
//        this.transactionRepository = transactionRepository;
//        this.userRepository = userRepository;
//    }
//
////    @GetMapping
////    public String paymentsPage(@AuthenticationPrincipal UserDetails userDetails,
////                               @RequestParam(required = false) TransactionType type,
////                               @RequestParam(required = false) LocalDateTime dateFrom,
////                               @RequestParam(required = false) LocalDateTime dateTo,
////                               Model model) {
////        User user = userRepository.findByEmail(userDetails.getUsername());
////
////        // Текущий баланс
////        model.addAttribute("currentBalance", user.getAccountBalance());
////
////        // История транзакций с фильтрами
////        List<Transaction> transactions = paymentService
////                .getUserTransactions(user, type, dateFrom, dateTo);
////        model.addAttribute("transactions", transactions);
////
////        // Статистика
////        model.addAttribute("totalIncome", paymentService.calculateTotalIncome(user));
////        model.addAttribute("totalExpense", paymentService.calculateTotalExpense(user));
////
////        return "consumer/payments";
////    }
//
////    @Transactional
////    public Transaction createDepositRequest(User user, BigDecimal amount) {
////        Transaction transaction = new Transaction();
////        transaction.setUser(user);
////        transaction.setAmount(amount);
////        transaction.setType(TransactionType.DEPOSIT);
////        transaction.setStatus(TransactionStatusEnum.PENDING);
////        transaction.setCreatedAt(LocalDateTime.now());
////        transaction.setDescription("Пополнение баланса");
////        return transactionRepository.save(transaction);
//}
