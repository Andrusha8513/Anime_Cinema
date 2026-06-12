package ara.repository;

import ara.entity.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepository<RefreshToken> {

    public Optional<RefreshToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId) {
        return list("userId = ?1 and revoked = false", userId);
    }

    public List<RefreshToken> findByUserId(UUID userId) {
        return list("userId", userId);
    }

    @Transactional
    public int revokeAllByUserId(UUID userId) {
        return update("revoked = true where userId = ?1 and revoked = false", userId);
    }

    @Transactional
    public long deleteExpiredTokens(LocalDateTime now) {
        return delete("expiresAt < ?1", now);
    }

    public long countByUserIdAndRevokedFalse(UUID userId) {
        return count("userId = ?1 and revoked = false", userId);
    }

    @Transactional
    public void persist(RefreshToken token) {
        PanacheRepository.super.persist(token);
    }

}
