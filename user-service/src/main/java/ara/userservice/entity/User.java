package ara.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements Persistable<UUID> {
    @Id
    @Column(name = "id" , updatable = false , nullable = false)
    private UUID id;

    @NotBlank(message = "имя пользователя не может быть пустым")
    @Size(min = 1 , max = 50 , message = "Имя пользователя слишком длинное")
    @Column(nullable = false , unique = true)
    private String username;

    @NotBlank(message = "Почта не может быть пустой!")
    @Size(max = 50 , message = "Почта слишком длинная")
    @Email(message = "Некорректный формат почты")
    @Column(nullable = false , unique = true)
    private String email;

    @Size(max = 50 , message = "Почта слишком длинная")
    @Email(message = "Некорректный формат почты")
    @Column(unique = true )
    private String pendingEmail;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createAt;


    @Transient
    private boolean isNewEntity = true;

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNewEntity = false; }
}
