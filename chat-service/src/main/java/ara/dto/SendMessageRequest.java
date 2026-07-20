package ara.dto;

import java.time.Instant;
import java.util.UUID;

public record SendMessageRequest(
        UUID conversationId,
        String content
) {}
