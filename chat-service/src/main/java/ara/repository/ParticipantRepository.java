package ara.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@ApplicationScoped
public class ParticipantRepository {

    private final CqlSession session;
    private PreparedStatement selectByConversation;
    private PreparedStatement checkParticipantStmt;
    private PreparedStatement selectRoleStmt;


    public ParticipantRepository(CqlSession session) {
        this.session = session;
    }

    void onStart(@Observes  @Priority(100) StartupEvent ev) {
        selectByConversation = session.prepare("""
            SELECT user_id FROM chat.participants_by_conversation
            WHERE conversation_id = ?
            """);

        checkParticipantStmt = session.prepare("""
                SELECT user_id FROM chat.participants_by_conversation
                WHERE conversation_id = ? AND user_id = ?
                """);

        selectRoleStmt = session.prepare(
                "SELECT role FROM chat.participants_by_conversation WHERE conversation_id = ? AND user_id = ?");
    }

   public List<UUID> findUserIds(UUID conversationId){
        ArrayList<UUID> ids = new ArrayList<>();
        for (Row row : session.execute(selectByConversation.bind(conversationId))){
            ids.add(row.getUuid("user_id"));
        }
        return  ids;
   }

   public Optional<String> findRole(UUID conversationId , UUID userId){
        Row row = session.execute(selectRoleStmt.bind(conversationId , userId)).one();
        return row == null ? Optional.empty() : Optional.ofNullable(row.getString("role"));
   }

//   public boolean isParticipant(UUID conversationId , UUID userId){
//        return  findUserIds(conversationId).contains(userId);
//   }

    public boolean isParticipant(UUID conversationId , UUID userId){
        BoundStatement bound = checkParticipantStmt.bind(conversationId  , userId);
        ResultSet resultSet = session.execute(bound);
        return resultSet.one() != null;
    }
}
