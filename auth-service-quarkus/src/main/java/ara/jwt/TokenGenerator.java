package ara.jwt;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TokenGenerator {

    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;


    @ConfigProperty(name = "app.access.private-key-location", defaultValue = "keys/auth_private_key_pkcs8.pem")
    String privateKeyLocation;

    private RSAPrivateKey privateKey;

    @PostConstruct
    void init() {
        this.privateKey = loadPrivateKey(privateKeyLocation);
    }

    public String generateAccessToken(TokenData tokenData) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(tokenData.userId().toString())
                .claim("upn", tokenData.userId().toString())
                .claim("groups", tokenData.roles().stream()
                        .map(Role::name).collect(Collectors.toSet()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ACCESS_TOKEN_LIFETIME)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    private RSAPrivateKey loadPrivateKey(String location) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(location)) {
            if (is == null) {
                throw new IllegalStateException("Приватный ключ не найден: " + location);
            }
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось загрузить приватный ключ: " + location, e);
        }
    }
}