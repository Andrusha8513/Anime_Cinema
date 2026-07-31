package ara.config;

import com.datastax.oss.driver.api.core.CqlSession;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;



@ApplicationScoped
public class ScyllaSchemaInitializer {

   private static final Logger log = Logger.getLogger(ScyllaSchemaInitializer.class);


    void onStart(@Observes @Priority(1) StartupEvent ev , CqlSession session){
        String schema = readSchema();
        Arrays.stream(schema.split(";"))
                .map(String::trim)
                .filter(stmt -> !stmt.isBlank())
                .forEach(stmt -> {
                    session.execute(stmt);
                    log.debugf("Выполнено: %s", stmt.split("\\(")[0].trim());
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
