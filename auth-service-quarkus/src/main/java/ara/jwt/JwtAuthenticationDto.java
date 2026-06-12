package ara.jwt;

public record JwtAuthenticationDto(
        String accessToken,
        String refreshToken
) {

}
