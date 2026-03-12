package ru.urfu.dto;

import lombok.*;

import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GoodsDto {
    @Id
    private Long id;

    @NotEmpty
    private String sellerName;


    private String sellerEmail;

    @NotEmpty
    private String name;

    @NotEmpty
    private String description;

    @Min(value = 0, message = "Цена не может быть отрицательной")
    private BigDecimal price;

    private String moderationStatus;

    private String imagePath;

    @NotEmpty
    private Long quantity;

}
