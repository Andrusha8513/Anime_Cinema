package ara.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationDto(
        @NotBlank(message = "имя пользователя не может быть пустым")
        @Size(min = 1 , max = 50 , message = "Имя пользователя слишком длинное")
        String username,
        @NotBlank(message = "Почта не может быть пустой!")
        @Size(max = 50 , message = "Почта слишком длинная")
        @Email(message = "Некорректный формат почты")
        String email,
        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов")
        String password
) {
}
