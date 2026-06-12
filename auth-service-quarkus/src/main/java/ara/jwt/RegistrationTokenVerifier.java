package ara.jwt;

import ara.exeption.InvalidRegistrationTokenException;
import ara.dto.RegistrationClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class RegistrationTokenVerifier {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "typ";
    private static final String TOKEN_TYPE = "registration";

    @ConfigProperty(name = "app.registration.public-key-location")
    String publicKeyLocation;

    @ConfigProperty(name = "app.registration.issuer")
    String expectedIssuer;

    @ConfigProperty(name = "app.registration.audience")
    String expectedAudience;

    private RSAPublicKey publicKey;

    void onStart(@Observes StartupEvent ev) {
        this.publicKey = loadPublicKey(publicKeyLocation);
    }

    public RegistrationClaims verify(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(expectedIssuer)
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidRegistrationTokenException("Регистрационный токен недействителен", e);
        }

        if (!TOKEN_TYPE.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new InvalidRegistrationTokenException("Неверный тип токена");
        }

        UUID userId;

        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new InvalidRegistrationTokenException("Некорректный userId в токене", e);
        }

        String username = claims.get(CLAIM_USERNAME, String.class);
        String email = claims.get(CLAIM_EMAIL, String.class);
        String jti = claims.getId();

        if (username == null || email == null || jti == null) {
            throw new InvalidRegistrationTokenException("В токене отсутствуют обязательные claim'ы");
        }

        return new RegistrationClaims(userId, username, email, jti);
    }

    private RSAPublicKey loadPublicKey(String location) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(location)) {
            if (is == null) {
                throw new IllegalStateException("Публичный ключ не найден: " + location);
            }
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить публичный ключ: " + location, e);
        }
    }




}

