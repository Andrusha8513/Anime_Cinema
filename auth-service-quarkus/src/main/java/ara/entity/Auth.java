package ara.entity;

import ara.jwt.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Auth {
    @Id
    @Column(nullable = false)
    private UUID userId;

    @NotBlank(message = "имя пользователя не может быть пустым")
    @Size(min = 1 , max = 50 , message = "Имя пользователя слишком длинное")
    @Column(nullable = false , unique = true)
    private String username;

    @NotBlank(message = "Почта не может быть пустой!")
    @Size(max = 50 , message = "Почта слишком длинная")
    @Email(message = "Некорректный формат почты")
    @Column(nullable = false , unique = true)
    private String email;

    private String password;

    private boolean enabled;
    @Column(name = "account_locked")
    private boolean accountLocked = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;



    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}