package ara.service;

import ara.dto.LogoutRequest;
import ara.dto.RegistrationClaims;
import ara.entity.Auth;
import ara.entity.RefreshToken;
import ara.jwt.*;
import ara.redis.JtiStore;
import ara.repository.AuthRepository;
import ara.repository.RefreshTokenRepository;
import com.password4j.Password;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Transactional
@ApplicationScoped
public class AuthService {

    //    private final RegistrationTokenRepository registrationTokenRepository;
    private static final Duration REGISTRATION_JTI_TTL = Duration.ofMinutes(5);

    private final AuthRepository authRepository;
    private final TokenGenerator tokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegistrationTokenVerifier registrationTokenVerifier;
    private final JtiStore jtiStore;


    public AuthService(AuthRepository authRepository, TokenGenerator tokenGenerator, RefreshTokenRepository refreshTokenRepository, RegistrationTokenVerifier registrationTokenVerifier, JtiStore jtiStore) {
        this.authRepository = authRepository;
        this.tokenGenerator = tokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
        this.registrationTokenVerifier = registrationTokenVerifier;
        this.jtiStore = jtiStore;
    }


    public JwtAuthenticationDto completeRegistration(String registrationToken, String password, String deviceInfo,
                                                     String ipAddress) {
        RegistrationClaims claims = registrationTokenVerifier.verify(registrationToken);
        UUID userId = claims.userId();

        //  лог успешной проверки токена и его метаданных
        log.info("complete-registration: verify OK, userId={}, jti={}", claims.userId(), claims.jti());

        Optional<Auth> existing = authRepository.findById(userId);
        if (existing.isPresent() && existing.get().isEnabled()) {
            throw new SecurityException("Пользователь уже зарегистрирован");
        }

        if (!jtiStore.markUsedIfAbsent(claims.jti(), REGISTRATION_JTI_TTL)) {
            throw new SecurityException("Токен регистрации уже был использован");
        }

        //  лог успешного сжигания JTI в Redis и перехода к созданию аккаунта
        log.info("complete-registration: jti отмечен, создаю Auth для {}", userId);

        Auth auth = existing.orElseGet(Auth::new);
        auth.setUserId(userId);
        auth.setUsername(claims.username());
        auth.setEmail(claims.email());
        auth.setRoles(Set.of(Role.USER));
        auth.setEnabled(true);

        String hashed = Password.hash(password).addRandomSalt().withArgon2().getResult();
        auth.setPassword(hashed);
        authRepository.persist(auth);

        log.info("Регистрация завершена для пользователя {}", userId);

        //  лог перед успешным возвратом токенов сессии
        log.info("complete-registration: успех, выданы токены для {}", userId);

        return issueSession(auth, deviceInfo, ipAddress);
    }


    public JwtAuthenticationDto login(String email, String password, String deviceInfo, String ipAddress) {
        Auth auth = authRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("Неверный email или пароль"));

        if (!auth.isEnabled()) {
            throw new SecurityException("Аккаунт не активирован. Установите пароль.");
        }
        if (!auth.isAccountNonLocked()) {
            throw new SecurityException("Аккаунт заблокирован");
        }

        boolean matches = Password.check(password, auth.getPassword()).withArgon2();

        if (!matches) {
            throw new SecurityException("Неверный email или пароль");
        }
        log.info("Пользователь {} успешно залогинился с устройства: {}", auth.getUserId(), deviceInfo);
        return issueSession(auth, deviceInfo, ipAddress);

    }

    ;


    public JwtAuthenticationDto refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new SecurityException("Refresh token не найден"));

        if (!refreshToken.isValid()) {
            throw new SecurityException("Refresh token истёк или отозван");
        }

        Auth auth = authRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new NotFoundException("Пользователь с таким id: " + refreshToken.getUserId() + " не найден"));

        if (!auth.isEnabled() || auth.isAccountNonLocked()) {
            throw new SecurityException("Пользователь  заблокирован");
        }

        TokenData tokenData = new TokenData(auth.getUserId(), auth.getRoles());
        String newAccessToken = tokenGenerator.generateAccessToken(tokenData);

        if (refreshToken.needsRenewal()) {
            refreshToken.renew();
            refreshTokenRepository.persist(refreshToken);
            log.info("Refresh token продлён для пользователя {} до {}",
                    auth.getUserId(), refreshToken.getExpiresAt());
        }

        return new JwtAuthenticationDto(newAccessToken, refreshTokenValue);

    }


    public void logout(LogoutRequest logoutRequest) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(logoutRequest.refreshToken())
                .orElseThrow(() -> new SecurityException("Refresh token не найден"));

        refreshToken.revoke();
        refreshTokenRepository.persist(refreshToken);

        log.info("Пользователь {} вышел с устройства: {}", refreshToken.getUserId(), refreshToken.getDeviceInfo());
    }


    public void logoutFromAllDevices(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        tokens.forEach(RefreshToken::revoke);
        refreshTokenRepository.persist(tokens);

        log.info("Пользователь {} вышел со всех устройств", userId);
    }

    public List<RefreshToken> getActiveSessions(UUID userId) {
        return refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
    }


    private JwtAuthenticationDto issueSession(Auth auth, String deviceInfo, String ipAddress) {
        TokenData tokenData = new TokenData(auth.getUserId(), auth.getRoles());
        String accessToken = tokenGenerator.generateAccessToken(tokenData);
        String refreshTokenValue = tokenGenerator.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(auth.getUserId())
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        refreshTokenRepository.persist(refreshToken);
        return new JwtAuthenticationDto(accessToken, refreshTokenValue);
    }


//    public void setPasswordRequest(SetPasswordRequest setPasswordRequest) {
//        RegistrationToken registrationToken = registrationTokenRepository.findByToken(setPasswordRequest.token())
//                .orElseThrow(() -> new NotFoundException("Токен регистрации не найден"));
//
//
//        if (registrationToken.isExpired()) {
//            registrationTokenRepository.delete(registrationToken);
//            throw new SecurityException("Аккаунт не активирован. Установите пароль.");
//        }
//
//        Auth auth = authRepository.findById(registrationToken.getUserId())
//                .orElseThrow(() -> new SecurityException("пользователь с таким id: " + registrationToken.getUserId() + " не найден"));
//
//
//        Hash hashedPassword = Password.hash(setPasswordRequest.password())
//                .addRandomSalt()
//                .withArgon2();
//
//        auth.setPassword(String.valueOf(hashedPassword));
//        authRepository.persist(auth);
//
//        refreshTokenRepository.delete(setPasswordRequest.token());
//    }

}
