package ara.userservice.component;

import ara.userservice.dto.ConfirmationRedisDto;
import ara.userservice.entity.OutboxEvent;
import ara.userservice.repository.OutboxRepository;
import ara.userservice.service.RedisEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisOutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final RedisEmailService redisEmailService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    private static final String EVENT_TYPE = "CONFIRMATION_CODE_SAVE";
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;


    @Scheduled(fixedDelayString = "${app.outbox.redis.processor-delay-ms:100}")
    public void processRedisOutboxEvents() {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        List<OutboxEvent> events = outboxRepository
                .findTop50ByTypeAndProcessedFalse(EVENT_TYPE, pageable);

        for (OutboxEvent event : events) {
            try {
                transactionTemplate.executeWithoutResult(status -> processEvent(event));
            } catch (Exception e) {
                log.error("КРИТИЧЕСКАЯ ОШИБКА: Не удалось обработать событие исходящих сообщений {}, количество повторных попыток ={}", event.getId(), event.getRetries(), e);
                if (event.getRetries() >= MAX_RETRIES) {
                    event.setProcessed(true);
                    event.setErrorMessage(e.getMessage());
                    outboxRepository.save(event);
                    log.warn("Событие {} перешло в состояние \"неудачно\" после {} повторных попыток.", event.getId(), MAX_RETRIES);
                } else {
                    event.setRetries(event.getRetries() + 1);
                    outboxRepository.save(event);
                }
            }
        }
    }



  private void processEvent(OutboxEvent event){
        ConfirmationRedisDto dto;
        try {
           String payload = event.getPayload();
           if(payload == null || payload.isBlank()){
               throw new IllegalStateException("Payload is empty for event " + event.getId());
           }
           dto = objectMapper.readValue(payload , ConfirmationRedisDto.class);
        }catch (JsonProcessingException e){
            event.setProcessed(true);
            event.setErrorMessage("Ошибка JSON" + e.getMessage());
            outboxRepository.save(event);
            return;
        }

        redisEmailService.saveConfirmationCode(dto.code(), dto.userId());
        event.setProcessed(true);
        outboxRepository.save(event);
  }
}
