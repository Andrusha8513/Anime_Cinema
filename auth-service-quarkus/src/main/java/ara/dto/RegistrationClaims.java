package ara.dto;

import java.util.UUID;

public record RegistrationClaims(
        UUID userId,
        String username,
        String email,
        String jti
) {}