package ara.userservice.component;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodeGenerator {
    private final SecureRandom random = new SecureRandom();

    public String generateConfirmationCode(){
        return String.format("%06d" , random.nextInt(1_000_000));
    }
    public String generateResetCode(){
        return String.format("%06d" , random.nextInt(1_000_000));
    }
}
