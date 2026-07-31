package ara.repository;

import ara.dto.Conversation;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationRepository {

    private final CqlSession session;
    private PreparedStatement
            insertConversation,
            selectConversation,
            insertParticipant,
            insertUserConversation,
            selectUserConversationIds,
            deleteUserConversation;

    public ConversationRepository(CqlSession session) {
        this.session = session;
    }

    void onStart(@Observes @Priority(100) StartupEvent ev) {
        insertConversation = session.prepare("""
                INSERT INTO chat.conversations (conversation_id, type, title, created_by, created_at)
                VALUES (?, ?, ?, ?, ?)
                """);

        selectConversation = session.prepare("""
                SELECT conversation_id, type, title, created_by, created_at
                FROM chat.conversations WHERE conversation_id = ?
                """);

        insertParticipant = session.prepare("""
                INSERT INTO chat.participants_by_conversation (conversation_id, user_id, role, joined_at)
                VALUES (?, ?, ?, ?)
                """);

        insertUserConversation = session.prepare("""
                INSERT INTO chat.conversations_by_user (user_id, conversation_id, conversation_type)
                VALUES (?, ?, ?)
                """);

        selectUserConversationIds = session.prepare("""
                SELECT conversation_id, conversation_type
                FROM chat.conversations_by_user WHERE user_id = ?
                """);

        deleteUserConversation = session.prepare("""
                DELETE FROM chat.conversations_by_user
                WHERE user_id = ? AND conversation_id = ?
                """);
    }

    public void createConversation(Conversation c) {
        session.execute(insertConversation.bind(
                c.conversationId(), c.type(), c.title(), c.createdBy(), c.createdAt()));
    }

    public void addParticipant(UUID conversationId, UUID userId, String role) {
        session.execute(insertParticipant.bind(conversationId, userId, role, Instant.now()));
    }

    /** INSERT здесь = upsert по (user_id, conversation_id) — повторный вызов не создаёт дубль. */
    public void addToUserList(UUID userId, UUID conversationId, String type) {
        session.execute(insertUserConversation.bind(userId, conversationId, type));
    }

    public List<UUID> findUserConversationIds(UUID userId) {
        List<UUID> ids = new ArrayList<>();
        for (Row row : session.execute(selectUserConversationIds.bind(userId))) {
            ids.add(row.getUuid("conversation_id"));
        }
        return ids;
    }

    //Понадобится, когда добавлю выход из чата / удаление диалога
    //забело сука
    public void removeFromUserList(UUID userId, UUID conversationId) {
        session.execute(deleteUserConversation.bind(userId, conversationId));
    }

    public Optional<Conversation> findById(UUID conversationId) {
        Row row = session.execute(selectConversation.bind(conversationId)).one();
        if (row == null) return Optional.empty();
        return Optional.of(new Conversation(
                row.getUuid("conversation_id"), row.getString("type"), row.getString("title"),
                row.getUuid("created_by"), row.getInstant("created_at")));
    }
}