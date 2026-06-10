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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final ProcessedEventRepository processedEventRepository;
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

            markEventAsProcessed(eventId);
            log.info("Письмо отправлено для {}", payload.email());
        }catch (Exception e){
            log.error("Ошибка отправки письма для eventId={}", eventId, e);
        }
    }

    @Transactional
    public boolean isEventProcessed(String eventId){
        return processedEventRepository.existsById(eventId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEventAsProcessed(String eventId){
        processedEventRepository.save(new ProcessedEvent(eventId));
    }
}
