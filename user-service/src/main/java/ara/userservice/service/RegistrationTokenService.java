package ara.userservice.service;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

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


@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationTokenService {

    private static  final String CLAIM_USERNAME = "username";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "typ";
    private static final String TOKEN_TYPE = "registration";

    private final ResourceLoader resourceLoader;

    @Value("${app.jwt.registration.private-key-location}")
    private String privateKeyLocation;

    @Value("${app.jwt.registration.key-id}")
    private String keyId;

    @Value("${app.jwt.registration.issuer}")
    private String issuer;

    @Value("${app.jwt.registration.audience}")
    private String audience;

    @Value("${app.jwt.registration.ttl-seconds}")
    private long ttlSeconds;

    private RSAPrivateKey privateKey;

    @PostConstruct
    void init(){
        this.privateKey = loadPrivateKey(privateKeyLocation);
        log.info("Регистрационный ключ подписи загружен (kid={})", keyId);
    }

    public String issue(UUID userId , String username , String email){
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USERNAME , username)
                .claim(CLAIM_EMAIL , email)
                .claim(CLAIM_TYPE , TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofSeconds(ttlSeconds))))
                .signWith(privateKey , Jwts.SIG.RS512)
                .compact();
    }

    private RSAPrivateKey loadPrivateKey(String location){
        try {
            Resource resource = resourceLoader.getResource(location);
            String pem;
            try(InputStream is = resource.getInputStream()) {
                pem = new String(is.readAllBytes() , StandardCharsets.UTF_8);
            }

            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        }catch (Exception e){
            throw new IllegalStateException(
                    "Не удалось загрузить приватный ключ регистрации: " + location, e);
        }
    }
}
