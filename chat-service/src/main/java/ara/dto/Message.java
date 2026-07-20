package ara.dto;

import java.time.Instant;
import java.util.UUID;

public record Message(
        UUID conversationId,
        int bucket,
        UUID messageId,
        UUID senderId,
        String content,
        String type,
        boolean deleted,
        Instant createdAt
) {}