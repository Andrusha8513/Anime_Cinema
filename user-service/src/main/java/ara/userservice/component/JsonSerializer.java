package ara.userservice.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonSerializer {

    private final ObjectMapper objectMapper;

    public String toJson(Object object){
        try {
            return  objectMapper.writeValueAsString(object);
        }catch (JsonProcessingException e){
            throw new RuntimeException("Ошибка сереализации JSONr", e);
        }
    }
}
