package ara.dto;

import java.util.UUID;

public record IncomingWsMessage(
        String type ,
        String token ,
        UUID conversationId ,
        String content
) {
}
