package ara.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDirectRequest(@NotBlank String username) {
}
