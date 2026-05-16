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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id" , updatable = false , nullable = false)
    private UUID id;

    @NotBlank(message = "имя пользователя не может быть пустым")
    @Size(min = 1 , max = 50 , message = "Имя пользователя слишком длинное")
    private String username;

    @NotBlank(message = "Почта не может быть пустой!")
    @Size(max = 50 , message = "Почта слишком длинная")
    @Email(message = "Некорректный формат почты")
    @Column(unique = true)
    private String email;

    @Size(max = 50 , message = "Почта слишком длинная")
    @Email(message = "Некорректный формат почты")
    @Column(unique = true , updatable = false)
    private String pendingEmail;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createAt;
}
