package ara.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "refresh_tokens" , indexes = {
        @Index(name = "idx_refresh_tokens_user_id"  , columnList = "user_id") ,
        @Index(name = "idx_refresh_tokens_expires_at" , columnList = "expires_at")
})
public class RefreshToken {

    @Id
    private String token;

    @Column(name = "user_id" , nullable = false)
    private UUID userId;

    @Column(name = "device_info" , nullable = false)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private  LocalDateTime createdAt;

    @Builder.Default
    private boolean revoked = false;


    public  boolean isExpired(){
        return LocalDateTime.now().isAfter(expiresAt);
    }


    public  boolean isValid(){
        return !revoked && !isExpired();
    }

    public  boolean needsRenewal(){
        return expiresAt.isBefore(LocalDateTime.now().plusDays(7));
    }

    public void renew(){
        this.expiresAt = LocalDateTime.now().plusDays(30);
    }

    public void  revoke(){
        this.revoked = true;
    }
}
