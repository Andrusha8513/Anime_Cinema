package ara.userservice.component;

import ara.userservice.dto.ConfirmEmailDto;
import ara.userservice.dto.ConfirmationRedisDto;
import ara.userservice.dto.RedisUserDto;
import ara.userservice.entity.OutboxEvent;
import ara.userservice.entity.User;
import ara.userservice.mapper.OutboxEventMapper;
import ara.userservice.repository.OutboxRepository;
import ara.userservice.repository.UserRepository;
import ara.userservice.service.RedisEmailService;
import ara.userservice.service.RedisUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisOutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final RedisEmailService redisEmailService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final EventPayloadFactory eventPayloadFactory;
    private final OutboxEventMapper outboxEventMapper;
    private final UserRepository userRepository;
    private final RedisUser redisUser;

    private static final String CONFIRMATION_CODE_SAVE = "CONFIRMATION_CODE_SAVE";
    private static final String CONFIRM_EMAIL_REQUEST = "CONFIRM_EMAIL_REQUEST";
    private static final String CONFIRM_NEW_EMAIL_REQUEST = "CONFIRM_NEW_EMAIL_REQUEST";
    private static final String SAVE_USER_TO_REDIS = "SAVE_USER_TO_REDIS";
    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelayString = "${app.outbox.redis.processor-delay-ms:100}")
    public void processRedisOutboxEvents() {
        processEventsOfType(SAVE_USER_TO_REDIS,         this::processUserSave);
        processEventsOfType(CONFIRMATION_CODE_SAVE, this::processCodeSaveEvent);
        processEventsOfType(CONFIRM_EMAIL_REQUEST, this::processConfirmEmailRequestEvent);
        processEventsOfType(CONFIRM_NEW_EMAIL_REQUEST, this::processConfirmNewEmailRequestEvent);

    }

    private void processUserSave(OutboxEvent event) {
        RedisUserDto dto;
        try {
            String payload = event.getPayload();
            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException("Payload is empty for event " + event.getId());
            }
            dto = objectMapper.readValue(payload, RedisUserDto.class);
        } catch (JsonProcessingException e) {
            event.setProcessed(true);
            event.setErrorMessage("JSON error: " + e.getMessage());
            outboxRepository.save(event);
            return;
        }

        UUID userId = UUID.fromString(event.getAggregateId());
        redisUser.saveUserToRedis(userId, dto);

        event.setProcessed(true);
        outboxRepository.save(event);
    }

    private void processEventsOfType(String eventType, Consumer<OutboxEvent> handler) {
        List<OutboxEvent> events = outboxRepository.findPendingEvents(eventType);

        for (OutboxEvent event : events) {
            try {
                transactionTemplate.executeWithoutResult(status -> handler.accept(event));
            } catch (Exception e) {
                log.error("КРИТИЧЕСКАЯ ОШИБКА: Не удалось обработать событие {} типа {}, retries={}",
                        event.getId(), event.getType(), event.getRetries(), e);
                if (event.getRetries() >= MAX_RETRIES) {
                    event.setProcessed(true);
                    event.setErrorMessage(e.getMessage());
                    outboxRepository.save(event);
                    log.warn("Событие {} перешло в состояние \"неудачно\" после {} повторных попыток.",
                            event.getId(), MAX_RETRIES);
                } else {
                    event.setRetries(event.getRetries() + 1);
                    outboxRepository.save(event);
                }
            }
        }
    }

    private void processCodeSaveEvent(OutboxEvent event) {
        ConfirmationRedisDto dto;
        try {
            String payload = event.getPayload();
            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException("Payload is empty for event " + event.getId());
            }
            dto = objectMapper.readValue(payload, ConfirmationRedisDto.class);
        } catch (JsonProcessingException e) {
            event.setProcessed(true);
            event.setErrorMessage("JSON error: " + e.getMessage());
            outboxRepository.save(event);
            return;
        }

        redisEmailService.saveConfirmationCode(dto.code(), dto.userId());
        event.setProcessed(true);
        outboxRepository.save(event);
    }

    private void processConfirmEmailRequestEvent(OutboxEvent event) {
        ConfirmEmailDto dto;
        try {
            String payload = event.getPayload();
            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException("Payload is empty for event " + event.getId());
            }
            dto = objectMapper.readValue(payload, ConfirmEmailDto.class);
        } catch (JsonProcessingException e) {
            event.setProcessed(true);
            event.setErrorMessage("JSON error: " + e.getMessage());
            outboxRepository.save(event);
            return;
        }

        UUID userId = redisEmailService.getUserIdByConfirmationCode(dto.code())
                .orElseThrow(() -> new IllegalStateException("Code not found or expired for event " + event.getId()));


        redisEmailService.deleteConfirmationCode(dto.code());

        String activationPayload = eventPayloadFactory.userActivationPayload(userId);
        outboxRepository.save(outboxEventMapper.toEmailConfirmedEvent(userId, activationPayload));

        event.setProcessed(true);
        outboxRepository.save(event);
    }

    private void processConfirmNewEmailRequestEvent(OutboxEvent event) {
        UUID userId = UUID.fromString(event.getAggregateId());

        ConfirmEmailDto dto;
        try {
            String payload = event.getPayload();
            if (payload == null || payload.isBlank()) {
                throw new IllegalStateException("Payload is empty for event " + event.getId());
            }
            dto = objectMapper.readValue(payload, ConfirmEmailDto.class);
        } catch (JsonProcessingException e) {
            event.setProcessed(true);
            event.setErrorMessage("JSON error: " + e.getMessage());
            outboxRepository.save(event);
            return;
        }

        UUID userIdFromRedis = redisEmailService.getUserIdByConfirmationCode(dto.code())
                .orElseThrow(() -> new IllegalStateException("Code not found or expired for event " + event.getId()));

        if (!userIdFromRedis.equals(userId)) {
            throw new IllegalStateException("Code does not belong to user " + userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с id " + userId + " не найден"));

        if (user.getPendingEmail() == null) {
            throw new IllegalStateException("No pending email for user " + userId);
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        userRepository.save(user);


        String changedPayload = eventPayloadFactory.emailChangedPayload(user.getId(), user.getEmail());
        outboxRepository.save(outboxEventMapper.toEmailChangedEvent(user.getId(), changedPayload));

        redisEmailService.deleteConfirmationCode(dto.code());

        event.setProcessed(true);
        outboxRepository.save(event);
    }


    private UUID validateAndDeleteCode(String code, UUID userId) {
        UUID userIdFormRedis = redisEmailService.getUserIdByConfirmationCode(code)
                .orElseThrow(() -> new IllegalStateException("Code not found or expired"));

        if (userId != null && !userIdFormRedis.equals(userId)) {
            throw new IllegalStateException("Code does not belong to user");
        }

        redisEmailService.deleteConfirmationCode(code);
        return userIdFormRedis;
    }
}