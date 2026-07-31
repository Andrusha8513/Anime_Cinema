package ara.dto;

import java.time.Instant;
import java.util.UUID;

public record OutgoingWsMessage(
        String type ,
        String error ,
        UUID conversationId,
        UUID messageId,
        UUID senderId,
        String senderName ,
        String content,
        Instant createdAt
) {
    public static OutgoingWsMessage authOk(){
        return new OutgoingWsMessage("auth_ok", null, null, null, null, null, null  , null);

    }

    public static OutgoingWsMessage error(String msg) {
        return new OutgoingWsMessage("error", msg, null, null, null, null, null , null);
    }

    public static OutgoingWsMessage of(Message m , String senderName) {
        return new OutgoingWsMessage("message", null, m.conversationId(),
                m.messageId(), m.senderId(), senderName ,m.content(), m.createdAt());
    }
}
