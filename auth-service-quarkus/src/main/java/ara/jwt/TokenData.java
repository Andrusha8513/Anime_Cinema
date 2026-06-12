package ara.jwt;



import java.util.Set;
import java.util.UUID;

public record TokenData(
        UUID userId,
        Set<Role> roles) {
}
