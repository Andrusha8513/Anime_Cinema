package ara.userservice.repository;

import ara.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User , Long> {
    Optional<User> findByEmail(String email);

   Optional<User>  findById(UUID userId);

    Optional<User> findByUsername(String username);


    boolean existsById(UUID userId);
}
