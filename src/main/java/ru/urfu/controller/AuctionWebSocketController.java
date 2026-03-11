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
                                     int amount,
                                     Principal principal) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        User bidder = userRepository.findByEmail(principal.getName());

        // Валидация: ставка должна быть выше текущей
        if (amount > auction.getCurrentHighestBid()) {
            Bid bid = new Bid();
            bid.setAuction(auction);
            bid.setBidder(bidder);
            bid.setAmount(amount);
            bid.setBidTime(LocalDateTime.now());
            bidRepository.save(bid);

            auction.setCurrentHighestBid(amount);
            auctionRepository.save(auction);

            // Возвращаем данные для всех подписчиков темы /topic/auction/{id}
            return new AuctionUpdateDto(auctionId, amount, bidder.getName());
        }
        return null; // Или вернуть ошибку
    }
}

