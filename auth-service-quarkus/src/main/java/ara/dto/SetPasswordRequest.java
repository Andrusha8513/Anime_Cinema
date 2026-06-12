package ara.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(String token,
                                 @NotBlank
                                 @Size(min = 8 , message = "Пароль не должен быть короче 9 символов")
                                 String password) {}
