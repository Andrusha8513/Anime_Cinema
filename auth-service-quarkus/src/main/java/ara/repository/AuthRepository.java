package ara.repository;

import ara.entity.Auth;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuthRepository implements PanacheRepository<Auth> {
    public Optional<Auth> findById(UUID userId){
        return find("userId" , userId).firstResultOptional();
    }

    public boolean existsById(UUID userId) {
        return count("userId  = ?1", userId) > 0;
    }



    public Optional<Auth> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }



}
