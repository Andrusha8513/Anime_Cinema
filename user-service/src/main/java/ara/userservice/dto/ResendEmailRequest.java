package ara.userservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResendEmailRequest(@NotNull UUID userId) {}