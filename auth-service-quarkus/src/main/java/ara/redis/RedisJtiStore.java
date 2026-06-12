package ara.redis;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;

@ApplicationScoped
public class RedisJtiStore implements JtiStore {

    private static final String KEY_PREFIX = "reg:jti:";

    private final ValueCommands<String, String> commands;

    public RedisJtiStore(RedisDataSource redisDataSource) {
        this.commands = redisDataSource.value(String.class);
    }

    @Override
    public boolean markUsedIfAbsent(String jti, Duration ttl) {
        String result = commands.setGet(
                KEY_PREFIX + jti,
                "1",
                new SetArgs().nx().ex(ttl)
        );

        return result == null;
    }
}