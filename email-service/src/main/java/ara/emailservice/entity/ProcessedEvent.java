package ara.emailservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "processed_events")
@NoArgsConstructor
public class ProcessedEvent {
    @Id
    private String id;

    @CreationTimestamp
    private LocalDateTime createdAt;

public ProcessedEvent(String id){
    this.id = id;
}
}
