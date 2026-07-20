package ara.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID conversationId,
        UUID messageId,
        UUID senderId,
        String content,
        Instant createdAt
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
                m.conversationId(), m.messageId(), m.senderId(),
                m.content(), m.createdAt());
    }
}