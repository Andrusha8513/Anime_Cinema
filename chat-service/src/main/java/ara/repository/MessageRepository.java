package ara.repository;

import ara.dto.Message;
import ara.utilita.BucketUtil;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.datastax.oss.driver.api.core.cql.PreparedStatement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MessageRepository {

    private final CqlSession session;

    private PreparedStatement insertStmt;
    private PreparedStatement selectRecentStmt;

    public MessageRepository(CqlSession session) {
        this.session = session;
    }

    void onsStart(@Observes  @Priority(100) StartupEvent ev) {
        insertStmt = session.prepare("""
                INSERT INTO chat.messages_by_conversation
                    (conversation_id, bucket, message_id, sender_id, content, type, deleted, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """);

        selectRecentStmt = session.prepare("""
                SELECT conversation_id, bucket, message_id, sender_id, content, type, deleted, created_at
                FROM chat.messages_by_conversation
                WHERE conversation_id = ? AND bucket = ?
                LIMIT ?
                """);
    }

    public Message save(UUID conversationId, UUID senderId, String content) {
        Instant now = Instant.now();
        int bucket = BucketUtil.bucketFor(now);
        UUID messageId = Uuids.timeBased();

        BoundStatement bound = insertStmt.bind(
                conversationId, bucket, messageId, senderId,
                content, "TEXT", false, now);
        session.execute(bound);

        return new Message(conversationId, bucket, messageId, senderId, content, "TEXT", false, now);
    }


    public List<Message> findRecent(UUID conversationId, int limit) {
        int bucket = BucketUtil.currentBucket();
        BoundStatement bound = selectRecentStmt.bind(conversationId, bucket, limit);
        ResultSet rs = session.execute(bound);

        List<Message> messages = new ArrayList<>();
        for (Row row : rs) {
            messages.add(mapRow(row));
        }
        return messages;
    }

    private Message mapRow(Row row) {
        return new Message(
                row.getUuid("conversation_id"),
                row.getInt("bucket"),
                row.getUuid("message_id"),
                row.getUuid("sender_id"),
                row.getString("content"),
                row.getString("type"),
                row.getBoolean("deleted"),
                row.getInstant("created_at")
        );
    }
}

