package ara.dto;

import java.time.Instant;
import java.util.UUID;

public record Conversation(
        UUID conversationId,
        String type,
        String title,
        UUID createdBy,
        Instant createdAt
) {}