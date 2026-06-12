package ara.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(@NotBlank String code) {}
