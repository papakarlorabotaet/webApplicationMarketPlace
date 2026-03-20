package ru.urfu.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.urfu.dto.AuctionUpdateDto;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Bid;
import ru.urfu.entity.User;
import ru.urfu.repository.AuctionRepository;
import ru.urfu.repository.BidRepository;
import ru.urfu.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class AuctionWebSocketController {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionWebSocketController(AuctionRepository auctionRepository,
                                      BidRepository bidRepository,
                                      UserRepository userRepository,
                                      SimpMessagingTemplate messagingTemplate) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/auction.bid/{auctionId}")
    @SendTo("/topic/auction/{auctionId}")
    public AuctionUpdateDto placeBid(@DestinationVariable Long auctionId,
                                     BigDecimal amount,
                                     Principal principal) {
        // 1. Извлекаем данные (Principal.getName() вернет email/usernam из Spring Security)
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Аукцион не найден"));
        User bidder = userRepository.findByEmail(principal.getName()).orElse(null);

        // 2. Логика проверки цены
        BigDecimal currentMax = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid()
                : BigDecimal.ZERO;

        if (amount.compareTo(currentMax) > 0) {
            // 3. Создаем ставку
            Bid bid = new Bid();
            bid.setAuction(auction);
            bid.setBidder(bidder);
            bid.setAmount(amount);
            bid.setBidTime(LocalDateTime.now());
            bidRepository.save(bid);

            // 4. Обновляем аукцион
            auction.setCurrentHighestBid(amount);
            auctionRepository.save(auction);

            // 5. Возвращаем DTO (в конструктор передаем имя, а не весь объект User)
            return new AuctionUpdateDto(auctionId, amount, bidder.getName(), bidder.getId());
        }

        return null; // Если ставка слишком низкая, подписчики ничего не получат
    }
}

