package ara.config;

import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.InetSocketAddress;

@Slf4j
@ApplicationScoped
public class ScyllaConfig {

    @ConfigProperty(name = "scylla.contact-points", defaultValue = "localhost:9042")
    String contactPoints;

    @ConfigProperty(name = "scylla.local-datacenter", defaultValue = "datacenter1")
    String localDatacenter;

    @Produces
    @Singleton
    public CqlSession cqlSession() {
        String[] hostPort = contactPoints.split(":");
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1])))
                .withLocalDatacenter(localDatacenter)
                .build();
    }
}
