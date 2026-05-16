package ara.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record EmailPayload(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("email") String email,
        @JsonProperty("emailType") String emailType,
        @JsonProperty("confirmationCode") String confirmationCode
) {}
