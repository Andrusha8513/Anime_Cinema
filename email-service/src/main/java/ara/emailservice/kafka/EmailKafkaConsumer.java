package ara.emailservice.kafka;

import ara.emailservice.dto.EmailPayload;
import ara.emailservice.service.EmailService;
import ara.emailservice.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "USER",
            groupId = "${spring.kafka.consumer.group-id}"
//            errorHandler = "kafkaListenerErrorHandler"
    )
    public void onEmailEvent(
            String payload,
            @Header("eventType") String eventType,
            @Header("id") String eventId,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (!"EMAIL_REGISTRATION".equals(eventType)) {
            log.debug("Пропускаем событие с типом: {}", eventType);
            return;
        }

        log.info("Получено сообщение: topic={}, partition={}, offset={}, eventId={}, eventType={}",
                topic, partition, offset, eventId, eventType);



        if (processedEventService.isEventProcessed(eventId)) {
            log.info("Событие {} уже обработано, пропускаем", eventId);
            return;
        }

        try {
            EmailPayload emailPayload = objectMapper.readValue(payload, EmailPayload.class);

            if (emailPayload.email() == null || emailPayload.email().isBlank()) {
                throw new IllegalArgumentException("В payload обязательно должен быть указан адрес электронной почты.");
            }

            emailService.sendConfirmationEmailAsync(emailPayload, eventId);
            log.info("Запущена асинхронная отправка письма для {}", emailPayload.email());

        } catch (IllegalArgumentException e) {

            log.error("Невалидный payload для события {}. Сообщение будет пропущено.", eventId, e);
            processedEventService.markAsProcessed(eventId);

        } catch (Exception e) {

            log.error(" Временная ошибка при обработке события {}. Будет повторная попытка.", eventId, e);
            throw new RuntimeException("Failed to process event " + eventId, e);
        }
    }
}