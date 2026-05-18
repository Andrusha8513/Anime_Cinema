package ara.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ConfirmationRedisDto(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("code") String code
) {}