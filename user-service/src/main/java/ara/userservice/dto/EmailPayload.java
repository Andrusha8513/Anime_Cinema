package ara.userservice.dto;

import ara.userservice.eventType.EmailType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record EmailPayload(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("email") String email,
        @JsonProperty("emailType") EmailType emailType,
        @JsonProperty("confirmationCode") String confirmationCode
) {}
