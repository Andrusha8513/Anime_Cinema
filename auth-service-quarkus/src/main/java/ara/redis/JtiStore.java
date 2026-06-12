package ara.redis;

import java.time.Duration;

public interface JtiStore {

    boolean markUsedIfAbsent(String jti, Duration ttl);
}
