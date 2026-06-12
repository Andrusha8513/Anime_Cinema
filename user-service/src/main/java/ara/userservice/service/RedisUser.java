package ara.userservice.service;

import ara.userservice.dto.RedisUserDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedisUser {

    private static final String USER_KEY_PREFIX = "user:pending";


    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.pending-user-ttl-minutes:30}")
    private long ttlMinutes;

    public void saveUserToRedis(UUID userId, RedisUserDto userDto) {
        try {
            String json = objectMapper.writeValueAsString(userDto);
            redisTemplate.opsForValue().set(key(userId)  , json , Duration.ofMinutes(ttlMinutes));
            log.debug("Pending-пользователь {} сохранён в Redis (ttl {} мин)", userId, ttlMinutes);
        }catch (JsonProcessingException e){
            throw new IllegalStateException("Не удалось сериализовать pending-пользователя " + userId, e);
        }
    }

    public Optional<RedisUserDto> getPendingUser(UUID userId){
        String json = redisTemplate.opsForValue().get(key(userId));
        if (json == null){
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json , RedisUserDto.class));
        }catch (JsonProcessingException e){
            log.error("Не удалось десериализовать pending-пользователя {}", userId, e);
            return Optional.empty();
        }
    }

    public void deletePendingUser(UUID userId){
        redisTemplate.delete(key(userId));
    }


    private String key(UUID userId){
        return USER_KEY_PREFIX + userId;
    }
}
