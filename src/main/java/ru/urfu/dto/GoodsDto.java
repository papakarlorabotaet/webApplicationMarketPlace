package ru.urfu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoodsDto {
    private Long id;

    @NotEmpty
    private String name;

    @NotEmpty
    private String description;

    @Min(value = 0, message = "Цена не может быть отрицательной")
    private int price;

    private String moderationStatus;

}
