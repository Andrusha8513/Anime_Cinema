package ara.userservice.dto;

public record RegistrationDto(
        String username,
        String email,
        String password
) {
}
