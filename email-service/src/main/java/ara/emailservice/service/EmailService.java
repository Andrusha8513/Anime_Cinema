package ara.emailservice.service;

import ara.emailservice.dto.EmailPayload;
import ara.emailservice.entity.ProcessedEvent;
import ara.emailservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final ProcessedEventService processedEventService;
    @Value("${spring.mail.username}")
    private String from;

    @Async
    public void sendConfirmationEmailAsync(EmailPayload payload , String eventId){
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(payload.email());
            message.setSubject("Подтверждение регистрации");
            message.setText("Ваш код подтверждения: " + payload.confirmationCode());
            mailSender.send(message);

            processedEventService.markAsProcessed(eventId);

            log.info("Письмо отправлено для {}", payload.email());
        }catch (Exception e){
            log.error("Ошибка отправки письма для eventId={}", eventId, e);
            throw  new RuntimeException("Не удалось отправить электронное письмо. Ошибка: "  , e);
        }
    }


}
