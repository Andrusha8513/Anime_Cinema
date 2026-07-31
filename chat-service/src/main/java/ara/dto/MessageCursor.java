package ara.dto;

import java.util.UUID;

public record MessageCursor(int  bucket , UUID messageId) {
}
