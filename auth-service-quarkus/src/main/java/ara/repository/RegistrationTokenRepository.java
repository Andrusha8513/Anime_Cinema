//package ara.repository;
//
//import ara.entity.RegistrationToken;
//import io.quarkus.hibernate.orm.panache.PanacheRepository;
//import jakarta.enterprise.context.ApplicationScoped;
//
//import java.util.Optional;
//@ApplicationScoped
//public class RegistrationTokenRepository implements PanacheRepository<RegistrationToken> {
//    public Optional<RegistrationToken> findByToken(String token){
//        return find("token" , token).firstResultOptional();
//    }
//}
