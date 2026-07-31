package ara.kafka;

import ara.entity.Auth;
import ara.repository.AuthRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class AuthKafkaConsumer {

    private static final Logger log = Logger.getLogger(AuthKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final AuthRepository authRepository;

    public AuthKafkaConsumer(ObjectMapper objectMapper, AuthRepository authRepository) {
        this.objectMapper = objectMapper;
        this.authRepository = authRepository;
    }

    @Incoming("auth-service-quarkus")
    @Blocking
    public CompletionStage<Void> onAuthEvent(IncomingKafkaRecord<String, String> record) {
        String eventType = header(record, "eventType");
        String eventId = header(record, "id");
        String payload = record.getPayload();

        log.infof("Получено сообщение: topic=%s, partition=%s, offset=%s, eventId=%s, eventType=%s",
                record.getTopic(), record.getPartition(), record.getOffset(), eventId, eventType);

        try {
            if (eventType == null) {
                log.warn("Событие без eventType, пропускаем");
                return record.ack();
            }

            switch (eventType) {
                case "EMAIL_CHANGED" -> updateEmail(payload);
                case "NEW_USERNAME" -> updateUsername(payload);
                default -> log.debugf("Пропускаем событие с типом: %s", eventType);
            }
            return record.ack();

        } catch (Exception e) {
            log.errorf(e, "Ошибка обработки события %s (%s), payload=%s", eventId, eventType, payload);
            return record.nack(e);
        }
    }

    @Transactional
    void updateEmail(String payload) throws Exception {
        JsonNode json = objectMapper.readTree(payload);
        UUID userId = requireUuid(json, "userId");
        String newEmail = requireText(json, "email");

        Auth auth = authRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Auth не найден для userId=" + userId));

        auth.setEmail(newEmail);
        authRepository.persist(auth);
        log.infof("Email обновлён: userId=%s -> %s", userId, newEmail);
    }

    @Transactional
    void updateUsername(String payload) throws Exception {
        JsonNode json = objectMapper.readTree(payload);
        UUID userId = requireUuid(json, "userId");
        String newUsername = requireText(json, "username");

        Auth auth = authRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Auth не найден для userId=" + userId));

        auth.setUsername(newUsername);
        authRepository.persist(auth);
        log.infof("Username обновлён: userId=%s -> %s", userId, newUsername);
    }

    private UUID requireUuid(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null) {
            throw new IllegalArgumentException("В payload нет поля " + field);
        }
        return UUID.fromString(node.asText());
    }

    private String requireText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null) {
            throw new IllegalArgumentException("В payload нет поля " + field);
        }
        return node.asText();
    }

    private String header(IncomingKafkaRecord<String, String> record, String name) {
        var h = record.getHeaders().lastHeader(name);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}