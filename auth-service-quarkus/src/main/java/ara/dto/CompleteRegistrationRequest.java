package ara.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRegistrationRequest(
        @NotBlank String token,
        @NotBlank
        @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов")
        String password
) {}