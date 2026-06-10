package ara.emailservice.repository;

import ara.emailservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent , UUID> {
    boolean existsById(String eventId);
}
