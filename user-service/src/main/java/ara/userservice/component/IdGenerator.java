package ara.userservice.component;


import com.fasterxml.uuid.NoArgGenerator;
import org.springframework.stereotype.Component;
import com.fasterxml.uuid.Generators;


import java.util.UUID;

@Component
public class IdGenerator {
private final NoArgGenerator generator = Generators.timeBasedEpochGenerator();
    public UUID newId(){
        return generator.generate();
    }
}
