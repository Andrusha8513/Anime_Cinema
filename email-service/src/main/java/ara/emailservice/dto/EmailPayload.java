package ara.emailservice.dto;

import ara.emailservice.emailType.EmailType;

import java.util.UUID;

public record EmailPayload(
        UUID userId,
        String email,
        EmailType emailTipe ,
        String confirmationCode
) {}
