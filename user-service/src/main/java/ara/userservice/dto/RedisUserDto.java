package ara.userservice.dto;

import java.util.UUID;

public record RedisUserDto(
        String username,
        String email
) {
}
