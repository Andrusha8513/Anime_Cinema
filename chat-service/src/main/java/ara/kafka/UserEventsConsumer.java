package ara.kafka;

import ara.repository.UserProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class UserEventsConsumer {

    private static final Logger log = Logger.getLogger(UserEventsConsumer.class);

    private final ObjectMapper objectMapper;
    private final UserProfileRepository userProfileRepository;

    public UserEventsConsumer(ObjectMapper objectMapper, UserProfileRepository userProfileRepository) {
        this.objectMapper = objectMapper;
        this.userProfileRepository = userProfileRepository;
    }

    @Incoming("user-events")
    @Blocking
    public CompletionStage<Void> onUserEvent(IncomingKafkaRecord<String, String> record) {
        String eventType = header(record, "eventType");
        String payload = record.getPayload();

        try {
            if (eventType == null) {
                log.warn("Событие без заголовка eventType, пропускаем");
                return record.ack();
            }

            switch (eventType) {
                case "USER_CREATED", "NEW_USERNAME" -> upsertProfile(payload, eventType);
                default -> log.debugf("Пропускаем событие: %s", eventType);
            }
            return record.ack();

        } catch (Exception e) {
            log.errorf(e, "Ошибка обработки события %s, payload=%s", eventType, payload);
            return record.nack(e);
        }
    }

    private void upsertProfile(String payload, String eventType) throws JsonProcessingException {
        JsonNode json = objectMapper.readTree(payload);
        JsonNode userIdNode = json.get("userId");
        JsonNode usernameNode = json.get("username");

        if (userIdNode == null || usernameNode == null) {
            throw new IllegalArgumentException("В payload нет userId или username");
        }

        UUID userId = UUID.fromString(userIdNode.asText());
        String username = usernameNode.asText();

        userProfileRepository.upsert(userId, username);
        log.infof("Профиль обновлён: eventType=%s, userId=%s, username=%s", eventType, userId, username);
    }

    private String header(IncomingKafkaRecord<String, String> record, String name) {
        var h = record.getHeaders().lastHeader(name);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}