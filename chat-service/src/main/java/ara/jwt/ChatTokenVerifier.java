package ara.jwt;

import ara.exeption.InvalidAccessTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class ChatTokenVerifier {

    @ConfigProperty(name = "app.jwt.access.public-key-location")
    String publicKeyLocation;

    @ConfigProperty(name = "app.jwt.access.issuer")
    String expectedIssuer;

    private RSAPublicKey publicKey;

    void onStart(@Observes StartupEvent ev) {
        this.publicKey = loadPublicKey(publicKeyLocation);
    }

    public UUID verifyBearerHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidAccessTokenException("Отсутствует Bearer-токен");
        }
        return verifyAndExtractUserId(authHeader.substring("Bearer ".length()));
    }


    public UUID verifyAndExtractUserId(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(expectedIssuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidAccessTokenException("Access-токен недействителен", e);
        }

        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new InvalidAccessTokenException("Некорректный userId в токене", e);
        }
    }


    private RSAPublicKey loadPublicKey(String location) {
        try {
            byte[] pemBytes;
            if (location.startsWith("file:")) {
                pemBytes = Files.readAllBytes(Path.of(location.substring("file:".length())));
            } else {
                String path = location.startsWith("classpath:")
                        ? location.substring("classpath:".length()) : location;
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                    if (is == null) {
                        throw new IllegalStateException("Публичный ключ не найден: " + location);
                    }
                    pemBytes = is.readAllBytes();
                }
            }
            String pem = new String(pemBytes, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить публичный ключ: " + location, e);
        }
    }
}