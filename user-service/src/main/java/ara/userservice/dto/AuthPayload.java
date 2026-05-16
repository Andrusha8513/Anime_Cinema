package ara.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AuthPayload(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("username") String username,
        @JsonProperty("email") String email,
        @JsonProperty("token") String token
) {}