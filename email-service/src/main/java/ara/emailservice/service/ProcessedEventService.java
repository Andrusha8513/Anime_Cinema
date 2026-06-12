package ara.emailservice.service;

import ara.emailservice.entity.ProcessedEvent;
import ara.emailservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean isEventProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    @Transactional
    public void markAsProcessed(String userId) {
        if (!processedEventRepository.existsById(userId)) {
            processedEventRepository.save(new ProcessedEvent(userId));
        }
    }
}