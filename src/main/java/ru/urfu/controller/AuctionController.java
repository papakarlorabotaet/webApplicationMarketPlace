package ru.urfu.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.urfu.entity.Auction;
import ru.urfu.entity.Bid;
import ru.urfu.entity.User;
import ru.urfu.repository.AuctionRepository;
import ru.urfu.repository.BidRepository;
import ru.urfu.repository.UserRepository;

import java.util.List;

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
    @GetMapping
    public String listAuctions(Model model) {
        List<Auction> activeAuctions = auctionRepository.findAllByIsActiveTrue();
        model.addAttribute("auctions", activeAuctions);
        return "auctions/list";
    }

    // Сделать ставку
    @PostMapping("/bid/{auctionId}")
    public String placeBid(@PathVariable Long auctionId,
                           @RequestParam int amount,
                           @AuthenticationPrincipal UserDetails userDetails) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        User bidder = userRepository.findByEmail(userDetails.getUsername());

        if (amount > auction.getCurrentHighestBid()) {
            Bid bid = new Bid();
            bid.setAuction(auction);
            bid.setBidder(bidder);
            bid.setAmount(amount);
            bidRepository.save(bid);

            auction.setCurrentHighestBid(amount);
            auctionRepository.save(auction);
        }
        return "redirect:/auctions";
    }
}