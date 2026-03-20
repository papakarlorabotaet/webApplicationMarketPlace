package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.urfu.dto.UserDto;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Bid;
import ru.urfu.entity.User;
import ru.urfu.repository.AuctionRepository;
import ru.urfu.repository.BidRepository;
import ru.urfu.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/auctions")
public class AuctionController {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    public AuctionController(AuctionRepository auctionRepository, BidRepository bidRepository, UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
    }

    // Список всех активных аукционов
    // Список всех активных аукционов
    @GetMapping
    public String listAuctions(Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {
        List<Auction> activeAuctions = auctionRepository.findAllByIsActiveTrue();
        model.addAttribute("auctions", activeAuctions);


        List<Long> auctionIds = activeAuctions.stream()
                .map(Auction::getId)
                .toList();
        model.addAttribute("auctionIds", auctionIds);  // ← ← ← Новое


        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            model.addAttribute("user", UserDto.fromEntity(user));
        }
        return "auctions/list";
    }

    // Сделать ставку
    @PostMapping("/bid/{auctionId}")
    public String placeBid(@PathVariable Long auctionId,
                           @RequestParam BigDecimal amount,  // ← 1. Изменили int на BigDecimal
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: аукцион не найден.");
            return "redirect:/auctions";
        }

        User bidder = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (bidder == null) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: пользователь не найден.");
            return "redirect:/auctions/" + auctionId;
        }

        if (bidder.getAccountBalance().compareTo(amount) < 0) {
            redirectAttributes.addFlashAttribute("error", "Недостаточно средств на балансе!");
            return "redirect:/auctions/" + auctionId;
        }

        BigDecimal currentMax = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid()
                : BigDecimal.ZERO;

        if (amount.compareTo(currentMax) <= 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Ваша ставка должна быть больше текущей (" + currentMax + ").");
            return "redirect:/auctions/" + auctionId;
        }

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bidRepository.save(bid);

        auction.setCurrentHighestBid(amount);
        auctionRepository.save(auction);

        redirectAttributes.addFlashAttribute("success", "Ваша ставка успешно принята!");
        return "redirect:/auctions/" + auctionId;
    }


    @GetMapping("/{auctionId}")
    public String viewAuction(@PathVariable Long auctionId, Model model) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Аукцион не найден"));

        // Получаем историю ставок (сортируем по убыванию времени - свежие первые)
        List<Bid> bidHistory = bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);

        // Получаем последнюю ставку
        Bid lastBid = bidHistory.isEmpty() ? null : bidHistory.get(0);

        //Вычисляем время до окончания
        String timeRemaining = calculateTimeRemaining(auction.getEndTime());
        boolean timeRemainingExpired = auction.getEndTime().isBefore(LocalDateTime.now());

        // Время последней ставки
        LocalDateTime lastBidTime = lastBid != null ? lastBid.getBidTime() : null;

        model.addAttribute("auction", auction);
        model.addAttribute("bidHistory", bidHistory);
        model.addAttribute("lastBid", lastBid);
        model.addAttribute("lastBidTime", lastBidTime);
        model.addAttribute("timeRemaining", timeRemaining);
        model.addAttribute("timeRemainingExpired", timeRemainingExpired);

        return "auctions/details";
    }

    // Метод для расчёта времени до окончания
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

        if (days > 0) {
            sb.append(days).append(" дн. ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append(" ч. ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append(" мин. ");
        }
        sb.append(seconds).append(" сек.");

        return sb.toString();
    }

    // Завершение аукциона досрочно (для продавца)
    @PostMapping("/{auctionId}/end")
    @ResponseBody
    public String endAuctionEarly(@PathVariable Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Аукцион не найден"));

        auction.setActive(false);
        auctionRepository.save(auction);

        return "OK";
    }
}