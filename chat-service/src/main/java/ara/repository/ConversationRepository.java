package ara.repository;

import ara.dto.Conversation;
import ara.dto.UserConversation;
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
    private PreparedStatement insertConversation, selectConversation,
            insertParticipant, insertUserConversation, updateLastMessage, selectUserConversations;

    public ConversationRepository(CqlSession session) { this.session = session; }

    void onStart(@Observes @Priority(100) StartupEvent ev) {
        insertConversation = session.prepare("""
            INSERT INTO chat.conversations (conversation_id, type, title, created_by, created_at)
            VALUES (?, ?, ?, ?, ?)
            """);
        selectConversation = session.prepare(
                "SELECT conversation_id, type, title, created_by, created_at FROM chat.conversations WHERE conversation_id = ?");
        insertParticipant = session.prepare("""
            INSERT INTO chat.participants_by_conversation (conversation_id, user_id, role, joined_at)
            VALUES (?, ?, ?, ?)
            """);
        insertUserConversation = session.prepare("""
            INSERT INTO chat.conversations_by_user (user_id, last_message_at, conversation_id, conversation_type, title)
            VALUES (?, ?, ?, ?, ?)
            """);
        selectUserConversations = session.prepare("""
            SELECT conversation_id, conversation_type, title, last_message_at
            FROM chat.conversations_by_user WHERE user_id = ?
            """);
    }

    public void createConversation(Conversation c) {
        session.execute(insertConversation.bind(
                c.conversationId(), c.type(), c.title(), c.createdBy(), c.createdAt()));
    }

    public void addParticipant(UUID conversationId, UUID userId, String role) {
        session.execute(insertParticipant.bind(conversationId, userId, role, Instant.now()));
    }

    public void addToUserList(UUID userId, UUID conversationId, String type, String title, Instant lastMessageAt) {
        session.execute(insertUserConversation.bind(userId, lastMessageAt, conversationId, type, title));
    }

    public Optional<Conversation> findById(UUID conversationId) {
        Row row = session.execute(selectConversation.bind(conversationId)).one();
        if (row == null) return Optional.empty();
        return Optional.of(new Conversation(
                row.getUuid("conversation_id"), row.getString("type"), row.getString("title"),
                row.getUuid("created_by"), row.getInstant("created_at")));
    }

    public List<UserConversation> findUserConversations(UUID userId) {
        List<UserConversation> list = new ArrayList<>();
        for (Row row : session.execute(selectUserConversations.bind(userId))) {
            list.add(new UserConversation(
                    row.getUuid("conversation_id"), row.getString("conversation_type"),
                    row.getString("title"), row.getInstant("last_message_at")));
        }
        return list;
    }
}
