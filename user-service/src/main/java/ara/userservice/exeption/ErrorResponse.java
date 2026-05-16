package ara.userservice.exeption;

import java.time.LocalDateTime;

public record ErrorResponse(
        String errorCode,
        String message,
        LocalDateTime timestamp,
        String path
) {
}
