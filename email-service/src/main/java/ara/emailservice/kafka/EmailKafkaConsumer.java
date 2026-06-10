package ara.emailservice.kafka;

import ara.emailservice.dto.EmailPayload;
import ara.emailservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @KafkaListener(topics = "user_service.USER")
    public void onEmailEvent(String payload,
                             @Header("eventType") String eventType,
                             @Header("id") String eventId){
        if (!"EMAIL_REGISTRATION".equals(eventType)){
            return;
        }
        if (emailService.isEventProcessed(eventId)){
            log.info("Событие {} уже обработано, пропускаем", eventId);
            return;
        }

        try {
            EmailPayload emailPayload = objectMapper.readValue(payload , EmailPayload.class);
            emailService.sendConfirmationEmailAsync(emailPayload  , eventId);
            log.info("Запущена асинхронная отправка для {}", emailPayload.email());
        }catch (Exception e) {
            log.error("Invalid payload for event {}", eventId, e);
            emailService.markEventAsProcessed(eventId);
        }
    }
}
