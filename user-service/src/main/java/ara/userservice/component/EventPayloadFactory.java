package ara.userservice.component;

import ara.userservice.dto.AuthPayload;
import ara.userservice.dto.ConfirmationRedisDto;
import ara.userservice.dto.EmailPayload;
import ara.userservice.dto.RedisUserDto;
import ara.userservice.eventType.EmailType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventPayloadFactory {

    private final JsonSerializer jsonSerializer;

    public String authRegistrationPayload(UUID userId, String username, String email, String token) {
        return jsonSerializer.toJson(new AuthPayload(userId, username, email, token));
    }

    public String emailRegistrationPayload(UUID userId, String email, String confirmationCode) {
        return jsonSerializer.toJson(new EmailPayload(userId, email, EmailType.REGISTRATION, confirmationCode));
    }

    public String codePayload(UUID userId , String code){
        return jsonSerializer.toJson(new ConfirmationRedisDto(userId, code));
    }

    public String userActivationPayload(UUID userId){
        return jsonSerializer.toJson(Map.of("userId" , userId));
    }

    public String confirmEmailPayload(String code) {
        return jsonSerializer.toJson(Map.of("code", code));
    }

    public String emailChangedPayload(UUID userId , String newEmail){
        return jsonSerializer.toJson(Map.of("userId" , userId  , "newEmail"  , newEmail));
    }

    public String  redisUserPayload(RedisUserDto dto){
        return jsonSerializer.toJson(dto);
    }
}
