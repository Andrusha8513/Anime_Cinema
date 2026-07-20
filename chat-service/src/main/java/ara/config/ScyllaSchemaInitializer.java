package ara.config;

import com.datastax.oss.driver.api.core.CqlSession;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@ApplicationScoped
public class ScyllaSchemaInitializer {



    void onStart(@Observes @Priority(1) StartupEvent ev , CqlSession session){
        String schema = readSchema();
        Arrays.stream(schema.split(";"))
                .map(String::trim)
                .filter(stmt -> !stmt.isBlank())
                .forEach(stmt -> {
                    session.execute(stmt);
                    log.debug("Выполнено: %s", stmt.split("\\(")[0].trim());
                });
        log.info("Схема ScyllaDB применена");
    }

    private String readSchema(){
        try(InputStream is  = getClass().getClassLoader().getResourceAsStream("schema.cql")) {
            if (is == null){
                throw new IllegalStateException("schema.cql не найден в resources");
            }
            return new String(is.readAllBytes() , StandardCharsets.UTF_8);
        }catch (Exception e){
            throw new IllegalStateException("Не удалось прочитать schema.cql", e);
        }
    }
}
