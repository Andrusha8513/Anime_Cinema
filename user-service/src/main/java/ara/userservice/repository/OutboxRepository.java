package ara.userservice.repository;

import ara.userservice.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent,Long> {

    @Query(value = """
    SELECT * FROM outbox_events
    WHERE type = :type AND processed = false
    ORDER BY created_at
    LIMIT 50
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<OutboxEvent> findPendingEvents(@Param("type") String type);
}
