package ara.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisEmailService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String CONFIRMATION_CODE_PREFIX = "email:confirm:";
    private static final String RESET_PASSWORD_PREFIX = "email:reset:";
    private static final String EMAIL_LIMIT_PREFIX = "email:limit:";


    public long incrementEmailCount(String email) {
        String key = EMAIL_LIMIT_PREFIX + email;
        long count = redisTemplate.opsForValue().increment(key, 1);
        if (count == 1) {
            redisTemplate.expire(key, 15, TimeUnit.MINUTES);
        }
        return count;
    }

    public void saveConfirmationCode(String code, UUID userId) {
        redisTemplate.opsForValue().set(
                CONFIRMATION_CODE_PREFIX + code,
                userId.toString(),
                15,
                TimeUnit.MINUTES
        );
    }


    public Optional<UUID> getUserIdByConfirmationCode(String code) {
        String userIdStr = redisTemplate.opsForValue().get(CONFIRMATION_CODE_PREFIX + code);
        if (userIdStr == null) return Optional.empty();
        return Optional.of(UUID.fromString(userIdStr));
    }


    public void deleteConfirmationCode(String code) {
        redisTemplate.delete(CONFIRMATION_CODE_PREFIX + code);
    }

    public void saveResetCode(String code, UUID userId) {
        redisTemplate.opsForValue().set(
                RESET_PASSWORD_PREFIX + code,
                userId.toString(),
                15,
                TimeUnit.MINUTES
        );
    }

    public Optional<UUID> getUserIdByResetCode(String code) {
        String userIdStr = redisTemplate.opsForValue().get(RESET_PASSWORD_PREFIX + code);
        if (userIdStr == null) return Optional.empty();
        return Optional.of(UUID.fromString(userIdStr));
    }

    public void deleteResetCode(String code) {
        redisTemplate.delete(RESET_PASSWORD_PREFIX + code);
    }

    public void markEmailConfirmed(UUID userId) {
        redisTemplate.opsForValue().set("email:confirmed:" + userId, "true", 1, TimeUnit.HOURS);
    }

    public boolean isEmailConfirmed(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("email:confirmed:" + userId));
    }
}