package ara.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfirmEmailDto(@JsonProperty("code") String code) {}