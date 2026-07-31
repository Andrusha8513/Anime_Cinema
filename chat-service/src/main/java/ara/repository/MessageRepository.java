package ara.repository;

import ara.dto.Message;
import ara.dto.MessageCursor;
import ara.dto.MessagePage;
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

    private static final int MAX_BUCKETS_PER_SCAN = 8;

    private final CqlSession session;

    private PreparedStatement insertStmt;
    private PreparedStatement selectRecentStmt;
    private PreparedStatement selectBeforeStmt;

    public MessageRepository(CqlSession session) {
        this.session = session;
    }

    void onStart(@Observes  @Priority(100) StartupEvent ev) {
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

        selectBeforeStmt = session.prepare("""
            SELECT conversation_id, bucket, message_id, sender_id, content, type, deleted, created_at
            FROM chat.messages_by_conversation
            WHERE conversation_id = ? AND bucket = ? AND message_id < ?
            LIMIT ?
            """);

    }




    public MessagePage findPage(UUID conversationId, int minBucket, MessageCursor cursor, int limit) {
        int bucket = (cursor != null) ? cursor.bucket() : BucketUtil.currentBucket();
        UUID before = (cursor != null) ? cursor.messageId() : null;

        List<Message> acc = new ArrayList<>(limit);
        int scanned = 0;

        while (bucket >= minBucket && acc.size() < limit && scanned < MAX_BUCKETS_PER_SCAN) {
            int need = limit - acc.size();

            BoundStatement bound = (before == null)
                    ? selectRecentStmt.bind(conversationId, bucket, need)
                    : selectBeforeStmt.bind(conversationId, bucket, before, need);

            for (Row row : session.execute(bound)) {
                acc.add(mapRow(row));
            }
            scanned++;

            if (acc.size() < limit) {
                bucket--;
                before = null;
            }
        }

        if (acc.isEmpty()) {
            boolean exhausted = bucket < minBucket;
            return new MessagePage(acc, exhausted ? null : new MessageCursor(bucket, null), !exhausted);
        }

        Message last = acc.get(acc.size() - 1);
        MessageCursor next = new MessageCursor(last.bucket(), last.messageId());
        boolean hasMore = last.bucket() > minBucket || acc.size() == limit;

        return new MessagePage(acc, hasMore ? next : null, hasMore);
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

