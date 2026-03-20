package ru.urfu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.urfu.dto.AuctionUpdateDto;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Bid;
import ru.urfu.entity.User;
import ru.urfu.repository.AuctionRepository;
import ru.urfu.repository.BidRepository;
import ru.urfu.repository.UserRepository;

import javax.transaction.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // Для отправки обновлений в WS

    @Transactional
    public void processBid(Long auctionId, int amount, String email) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Аукцион не найден"));

        User bidder = userRepository.findByEmail(email).orElse(null);


        BigDecimal bidAmount = BigDecimal.valueOf(amount);
        BigDecimal currentMax = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;

        // 1. Проверки
        if (bidAmount.compareTo(currentMax) <= 0) return; // Или кинуть ошибку для WS
        if (bidder.getAccountBalance().compareTo(bidAmount) < 0) return;

        // 2. ВОЗВРАТ денег предыдущему участнику
        if (auction.getCurrentHighestBidder() != null) {
            User prevBidder = auction.getCurrentHighestBidder();
            prevBidder.setAccountBalance(prevBidder.getAccountBalance().add(auction.getCurrentHighestBid()));
            userRepository.save(prevBidder);
        }

        // 3. СПИСАНИЕ денег у нового участника
        bidder.setAccountBalance(bidder.getAccountBalance().subtract(bidAmount));
        userRepository.save(bidder);

        // 4. СОХРАНЕНИЕ ставки
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(bidAmount);
        bidRepository.save(bid);

        // 5. ОБНОВЛЕНИЕ аукциона
        auction.setCurrentHighestBid(bidAmount);
        auction.setCurrentHighestBidder(bidder);
        auctionRepository.save(auction);

        // 6. ОПОВЕЩЕНИЕ всех через WebSocket
        // Отправляем в /topic/auction/{id}, на который подписан JS
        AuctionUpdateDto update = new AuctionUpdateDto(
                auction.getId(),
                bidAmount,
                bidder.getName(),
                bidder.getId()// Передаем имя для фронтенда
        );
        messagingTemplate.convertAndSend("/topic/auction/" + auctionId, update);
    }
}