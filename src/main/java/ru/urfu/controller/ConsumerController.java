package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.urfu.dto.UserDto;
import ru.urfu.entity.*;
import ru.urfu.repository.AuctionRepository;
import ru.urfu.repository.BidRepository;
import ru.urfu.repository.UserRepository;
import ru.urfu.service.OrderService;
import ru.urfu.service.PaymentService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/consumer")
public class ConsumerController {

    private final UserRepository userRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;

    public ConsumerController(UserRepository userRepository, OrderService orderService, PaymentService paymentService, BidRepository bidRepository, AuctionRepository auctionRepository) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        List<Order> recentOrders = orderService.findOrdersBySeller(user);
        model.addAttribute("user", user);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("cartItemsCount", 0); // позже
        return "consumer/profileConsumer";
    }

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
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
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        paymentService.createDepositRequest(user, amount);
        return "redirect:/consumer/payments?success";
    }

    @GetMapping("/payments")
    public String paymentsPage(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(required = false) TransactionType type,
                               @RequestParam(required = false) LocalDateTime dateFrom,
                               @RequestParam(required = false) LocalDateTime dateTo,
                               Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);

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


    // ✅ Список всех активных аукционов для потребителя
    @GetMapping("/auctions")
    public String listAuctions(Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {
        List<Auction> activeAuctions = auctionRepository.findAllByIsActiveTrue();
        model.addAttribute("auctions", activeAuctions);
        model.addAttribute("auctionIds", activeAuctions.stream().map(Auction::getId).toList());

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null) {
                model.addAttribute("user", UserDto.fromEntity(user));
                model.addAttribute("balance", user.getAccountBalance());
            }
        }
        return "redirect:/auction/list";
    }

    //  Детали аукциона для потребителя
    @GetMapping("/auctions/{auctionId}")
    public String viewAuction(@PathVariable Long auctionId,
                              Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Аукцион не найден"));

        // История ставок
        List<Bid> bidHistory = bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);
        Bid lastBid = bidHistory.isEmpty() ? null : bidHistory.get(0);

        // Время до окончания
        String timeRemaining = calculateTimeRemaining(auction.getEndTime());
        boolean timeRemainingExpired = auction.getEndTime().isBefore(LocalDateTime.now());

        // Данные текущего пользователя
        Long currentUserId = null;
        Boolean isCurrentUserLeader = false;
        BigDecimal userBalance = BigDecimal.ZERO;
        BigDecimal minBidAmount = auction.getStartingPrice();

        if (userDetails != null) {
            User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            if (currentUser != null) {
                currentUserId = currentUser.getId();
                userBalance = currentUser.getAccountBalance();

                // Проверяем, является ли текущий пользователь лидером
                if (auction.getCurrentHighestBidder() != null) {
                    isCurrentUserLeader = auction.getCurrentHighestBidder().getId().equals(currentUserId);
                }

                // Минимальная ставка (текущая + шаг)
                if (auction.getCurrentHighestBid() != null) {
                    minBidAmount = auction.getCurrentHighestBid().add(new BigDecimal("100"));
                }
            }
        }

        model.addAttribute("auction", auction);
        model.addAttribute("bidHistory", bidHistory);
        model.addAttribute("lastBid", lastBid);
        model.addAttribute("lastBidTime", lastBid != null ? lastBid.getBidTime() : null);
        model.addAttribute("timeRemaining", timeRemaining);
        model.addAttribute("timeRemainingExpired", timeRemainingExpired);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("isCurrentUserLeader", isCurrentUserLeader);
        model.addAttribute("userBalance", userBalance);
        model.addAttribute("minBidAmount", minBidAmount);

        return "consumer/auction-details";
    }

    //  Сделать ставку (POST)
    @PostMapping("/auctions/{auctionId}/bid")
    public String placeBid(@PathVariable Long auctionId,
                           @RequestParam BigDecimal amount,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "Необходимо авторизоваться!");
            return "redirect:/login";
        }

        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null || !auction.isActive()) {
            redirectAttributes.addFlashAttribute("error", "Аукцион не найден или завершён.");
            return "redirect:/consumer/auctions/" + auctionId;
        }

        User bidder = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (bidder == null) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден.");
            return "redirect:/consumer/auctions/" + auctionId;
        }

        // Проверка баланса
        if (bidder.getAccountBalance().compareTo(amount) < 0) {
            redirectAttributes.addFlashAttribute("error", "Недостаточно средств на балансе!");
            return "redirect:/consumer/auctions/" + auctionId;
        }

        // Проверка размера ставки
        BigDecimal currentMax = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid()
                : auction.getStartingPrice();

        if (amount.compareTo(currentMax) <= 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Ставка должна быть больше текущей (₽ " + currentMax + ").");
            return "redirect:/consumer/auctions/" + auctionId;
        }

        // Создаём ставку
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bidRepository.save(bid);

        // Обновляем аукцион
        auction.setCurrentHighestBid(amount);
        auction.setCurrentHighestBidder(bidder);
        auctionRepository.save(auction);

        redirectAttributes.addFlashAttribute("success", "Ставка ₽ " + amount + " принята! Вы лидер! 🏆");
        return "redirect:/consumer/auctions/" + auctionId;
    }

    // ✅ Мои активные аукционы (где пользователь сделал ставки)
    @GetMapping("/my")
    public String myAuctions(Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        List<Auction> myAuctions = auctionRepository.findAuctionsWhereUserBid(user.getId());
        model.addAttribute("auctions", myAuctions);
        model.addAttribute("auctionIds", myAuctions.stream().map(Auction::getId).toList());

        return "consumer/auctions/my";
    }

    // ✅ Расчёт времени до окончания
    private String calculateTimeRemaining(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (endTime.isBefore(now) || endTime.isEqual(now)) {
            return "Аукцион завершён";
        }

        Duration duration = Duration.between(now, endTime);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" дн. ");
        if (hours > 0 || days > 0) sb.append(hours).append(" ч. ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append(" мин. ");
        sb.append(seconds).append(" сек.");

        return sb.toString();
    }




}