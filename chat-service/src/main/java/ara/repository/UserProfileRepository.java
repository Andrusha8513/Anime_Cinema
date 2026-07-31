package ara.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


@ApplicationScoped
public class UserProfileRepository {

    private static final Logger log = Logger.getLogger(UserProfileRepository.class);
    private final CqlSession session;

    private PreparedStatement upsertProfile, upsertByUsername, selectById, selectByUsername, deleteByUsername;

    public UserProfileRepository(CqlSession session) {
        this.session = session;
    }

    void onStart(@Observes @Priority(100) StartupEvent ev) {
        upsertProfile = session.prepare("INSERT INTO chat.user_profiles (user_id , username) VALUES(? , ?)");
        upsertByUsername = session.prepare("INSERT INTO chat.users_by_username (username , user_id) VALUES(? , ?)");
        selectById = session.prepare("SELECT username FROM chat.user_profiles WHERE user_id = ?");
        selectByUsername = session.prepare("SELECT user_id FROM chat.users_by_username WHERE username = ?");
        deleteByUsername = session.prepare("DELETE FROM chat.users_by_username WHERE username=?");
    }


    public void upsert(UUID userId, String username) {
        findUsername(userId).ifPresent(old -> {
            if (!old.equals(username)) {
                session.execute(deleteByUsername.bind(old));
            }
        });

        session.execute(upsertProfile.bind(userId, username));
        session.execute(upsertByUsername.bind(username, userId));
    }


    public Optional<String> findUsername(UUID userId) {
        Row row = session.execute(selectById.bind(userId)).one();
        return row == null ? Optional.empty() : Optional.ofNullable(row.getString("username"));
    }

    public Optional<UUID> findUserIdByUsername(String username) {
        Row row = session.execute(selectByUsername.bind(username)).one();
        return row == null ? Optional.empty() : Optional.ofNullable(row.getUuid("user_id"));
    }

    public Map<String, UUID> findUserIdsByUsernames(List<String> usernames) {
        Map<String, UUID> resultMap = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = usernames.stream()
                .map(username -> session.executeAsync(selectByUsername.bind(username))
                        .thenAccept((AsyncResultSet asyncResultSet) -> {
                            Row row = asyncResultSet.one();
                            if (row != null) {
                                UUID userId = row.getUuid("user_id");
                                if (userId != null) {
                                    resultMap.put(username, userId);
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            log.warnf("Ошибка резолва %s: %s", username, ex.getMessage());
                            return null;
                        })
                        .toCompletableFuture()
                )
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return resultMap;
    }


}
