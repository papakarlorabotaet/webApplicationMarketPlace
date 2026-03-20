package ru.urfu.dto;

import lombok.*;
import ru.urfu.entity.Role;
import ru.urfu.entity.User;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

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
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    @NotEmpty(message = "Password should not be empty")
    private String name;
    private String surname;
    private String city;
    private String role;  // ← Для отображения ролей

    // ← ← ← Метод конвертации Entity → DTO
    public static UserDto fromEntity(User user) {
        if (user == null) return null;

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setCity(user.getCity());

        // Конвертируем роли в имена (без рекурсии!)
        if (user.getRoles() != null) {
            dto.setRole(user.getRoles().stream()
                    .map(Role::getName)
                    .toList().toString());
        }

        return dto;
    }



}