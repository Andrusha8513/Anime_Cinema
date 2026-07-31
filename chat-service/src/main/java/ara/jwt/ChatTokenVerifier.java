package ara.jwt;

import ara.exeption.InvalidAccessTokenException;
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
public class ChatTokenVerifier {

    @ConfigProperty(name = "app.jwt.access.public-key-location")
    String publicKeyLocation;

    @ConfigProperty(name = "app.jwt.access.issuer")
    String expectedIssuer;

    private PublicKey publicKey;

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


    private PublicKey loadPublicKey(String location) {
        try {
            String pemContent = readPem(location);
            try (PEMParser parser = new PEMParser(new StringReader(pemContent))) {
                Object obj = parser.readObject();
                if (obj == null) {
                    throw new IllegalStateException("Пустой или некорректный PEM: " + location);
                }
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                if (obj instanceof SubjectPublicKeyInfo spki) {
                    return converter.getPublicKey(spki);
                }
                if (obj instanceof X509CertificateHolder cert) {
                    return converter.getPublicKey(cert.getSubjectPublicKeyInfo());
                }
                throw new IllegalStateException(
                        "Неподдерживаемый тип PEM (" + obj.getClass().getSimpleName() + "): " + location);
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