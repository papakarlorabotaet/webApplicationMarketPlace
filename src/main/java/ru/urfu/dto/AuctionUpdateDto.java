package ru.urfu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class AuctionUpdateDto {
    // Геттеры
    private Long auctionId;
    private BigDecimal amount;
    private String bidderName;
    private Long bidderId;


}