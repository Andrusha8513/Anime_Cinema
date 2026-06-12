//package ara.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "registration_tokens", indexes = {
//        @Index(name = "idx_registration_tokens_token", columnList = "token"),
//        @Index(name = "idx_registration_tokens_expires_at", columnList = "expires_at")
//})
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@Getter
//@Setter
//public class RegistrationToken {
//
//    @Id
//    @Column(name = "user_id", nullable = false)
//    private UUID userId;
//
//    @Column(name = "token", nullable = false, length = 255)
//    private String token;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "expires_at")
//    private LocalDateTime expiresAt;
//
//    @Column(nullable = false)
//    @Builder.Default
//    private boolean revoked = false;
//
//
//
//    public boolean isExpired() {
//        return LocalDateTime.now().isAfter(expiresAt);
//    }
//
//    public void revoke() {
//        this.revoked = true;
//    }
//
//
//    public boolean isValid() {
//        return !revoked && !isExpired();
//    }
//
//    public boolean needsRenewal() {
//        return expiresAt.isBefore(LocalDateTime.now().plusDays(7));
//    }
//
//    public void renew(){
//        this.expiresAt = LocalDateTime.now().plusDays(30);
//    }
//
//}