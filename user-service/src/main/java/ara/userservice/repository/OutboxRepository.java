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

    @Query("SELECT e FROM OutboxEvent e WHERE e.type = :type AND e.processed = false ORDER BY e.createdAt")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findTop50ByTypeAndProcessedFalse(@Param("type") String type, Pageable pageable);
}
