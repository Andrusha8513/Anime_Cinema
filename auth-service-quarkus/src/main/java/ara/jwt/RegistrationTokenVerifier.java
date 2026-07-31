package ara.jwt;

import ara.exeption.InvalidRegistrationTokenException;
import ara.dto.RegistrationClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
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

    private PublicKey publicKey;

    void onStart(@Observes StartupEvent ev) {
        this.publicKey = loadPublicKey(publicKeyLocation);
    }

    public RegistrationClaims verify(String token) {
        Claims claims = parseToken(token);

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

    private Claims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(expectedIssuer)
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TOKEN_TYPE.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new InvalidRegistrationTokenException("Неверный тип токена");
            }

            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidRegistrationTokenException("Регистрационный токен недействителен", e);
        }
    }

    private PublicKey loadPublicKey(String location) {
        try {
            String pemContext = readPem(location);
            try (PEMParser parser = new PEMParser(new StringReader(pemContext))) {
                Object object = parser.readObject();
                if (object == null) {
                    throw new IllegalStateException("Пустой или некорректный PEM: " + location);
                }
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

                if (object instanceof SubjectPublicKeyInfo sup) {
                    return converter.getPublicKey(sup);
                }

                if (object instanceof X509CertificateHolder cert) {
                    return converter.getPublicKey(cert.getSubjectPublicKeyInfo());
                }
                throw new IllegalStateException(
                        "Неподдерживаемый тип PEM (" + object.getClass().getSimpleName() + "): " + location);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить публичный ключ: " + location, e);
        }
    }

    private String readPem(String location) throws IOException {
        byte[] bytes;
        if (location.startsWith("file:")) {
            bytes = Files.readAllBytes(Path.of(location.substring("file:".length())));
        } else {
            String path = location.startsWith("classpath:")
                    ? location.substring("classpath:".length()) : location;

            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalStateException("Публичный ключ не найден: " + location);
                }
                bytes = is.readAllBytes();
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}