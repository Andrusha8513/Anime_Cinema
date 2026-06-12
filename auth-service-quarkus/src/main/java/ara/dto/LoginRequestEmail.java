package ara.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestEmail(@Email @NotBlank String email, @NotBlank String password) {}
