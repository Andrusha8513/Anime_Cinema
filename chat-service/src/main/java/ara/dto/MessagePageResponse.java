package ara.dto;


import java.util.List;
import java.util.UUID;

public record MessagePageResponse(
        List<MessageResponse> messages,
        Integer nextBucket,
        UUID nextMessageId,
        boolean hasMore
) {
    public static MessagePageResponse from(MessagePage page) {
        return new MessagePageResponse(
                page.messages().stream().map(MessageResponse::from).toList(),
                page.nextCursor() != null ? page.nextCursor().bucket() : null,
                page.nextCursor() != null ? page.nextCursor().messageId() : null,
                page.hasMore());
    }
}