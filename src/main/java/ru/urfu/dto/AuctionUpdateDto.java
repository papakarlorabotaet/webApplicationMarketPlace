package ru.urfu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuctionUpdateDto {
    private Long auctionId;
    private int newPrice;
    private String bidderName;

    // Геттеры
    public Long getAuctionId() { return auctionId; }
    public int getNewPrice() { return newPrice; }
    public String getBidderName() { return bidderName; }
}