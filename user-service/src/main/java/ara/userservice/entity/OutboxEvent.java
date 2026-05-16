package ara.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String aggregateType; // "USER" или "AUTH" (помогает Debezium маршрутизировать топики)

    @Column(nullable = false)
    private String type; // Тип события, например: "USER_REGISTERED", "EMAIL_VERIFICATION_REQUSTED"

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload; // Весь твой JSON (username, email, code и т.д.) без пароля!

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

