package ara.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(@NotBlank @Size(min = 1, max = 50) String newUsername) {}
