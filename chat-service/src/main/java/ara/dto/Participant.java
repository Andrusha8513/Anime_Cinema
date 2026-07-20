package ara.dto;

import java.time.Instant;
import java.util.UUID;

public record Participant(
        UUID conversationId,
        UUID userId,
        String role,
        String customTitle,
        Instant joinedAt
) {}
