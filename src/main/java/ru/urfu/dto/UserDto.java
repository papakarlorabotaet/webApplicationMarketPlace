package ru.urfu.dto;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    @NotEmpty(message = "Email should not be empty")
    @Email
    private String email;
    @NotEmpty(message = "Password should not be empty")
    private String password;
    private String name;
    private String surname;
    private String city;
    // Можно добавить тип аккаунта (BUYER, SELLER), чтобы назначить роль при регистрации
    private String role;
}