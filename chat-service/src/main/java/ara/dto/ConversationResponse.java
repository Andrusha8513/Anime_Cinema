package ara.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(UUID conversationId, String type, String title, Instant createdAt) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.conversationId(), c.type(), c.title(), c.createdAt());
    }
}