package ara.web_socket;

import ara.dto.IncomingWsMessage;
import ara.dto.Message;
import ara.dto.OutgoingWsMessage;
import ara.exeption.NotParticipantException;
import ara.jwt.ChatTokenVerifier;
import ara.repository.MessageRepository;
import ara.repository.ParticipantRepository;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;



@ApplicationScoped
public class ChatMessageService {

    private static final Logger log = Logger.getLogger(ChatMessageService.class);


    private final ChatTokenVerifier verifier;
    private final ConnectionRegistry registry;
    private final MessageRepository messageRepository;
    private final ParticipantRepository participantRepository;
    private final OpenConnections openConnections;

    public ChatMessageService(MessageRepository messageRepository,
                              ChatTokenVerifier verifier,
                              ConnectionRegistry registry,
                              ParticipantRepository participantRepository,
                              OpenConnections openConnections) {
        this.messageRepository = messageRepository;
        this.verifier = verifier;
        this.registry = registry;
        this.participantRepository = participantRepository;
        this.openConnections = openConnections;
    }

    public void authenticate(WebSocketConnection connection, IncomingWsMessage message) {
        if (message.token() == null || message.token().isBlank()) {
            connection.sendTextAndAwait(OutgoingWsMessage.error("Токен обязателен"));
            connection.closeAndAwait();
            return;
        }


        UUID userId = verifier.verifyAndExtractUserId(message.token());
        registry.register(connection.id(), userId);
        log.infof("Пользователь %s аутентифицирован в сокете %s", userId, connection);
        connection.sendTextAndAwait(OutgoingWsMessage.authOk());
    }

    public void handleIncoming(WebSocketConnection connection, IncomingWsMessage message) {
        Optional<UUID> userOpt = registry.userOf(connection.id());

        if (userOpt.isEmpty()){
            connection.sendTextAndAwait(OutgoingWsMessage.error("Сначала аутентифицируйтесь"));
            return;
        }

        UUID senderId = userOpt.get();
        UUID conversationId = message.conversationId();

        if (!participantRepository.isParticipant(conversationId , senderId)){
            connection.sendTextAndAwait(OutgoingWsMessage.error("Вы не участник этого чата"));
            return;
        }

        Message saved = messageRepository.save(conversationId , senderId , message.content());
        broadcast(conversationId , OutgoingWsMessage.of(saved));
    }

    private void broadcast(UUID conversationId , OutgoingWsMessage out){
        List<UUID> participants = participantRepository.findUserIds(conversationId);
        log.infof("broadcast: чат %s , участников %s" , conversationId , participants.size());

        for (UUID participantId  : participants){
            Optional<WebSocketConnection> conn = registry.connectionIdOf(participantId)
                    .flatMap(openConnections::findByConnectionId);

            conn.ifPresentOrElse(
                    connection -> {
                        connection.sendTextAndAwait(out);
                        log.infof("доставленно -> %s"   , participantId);
                    } ,
                    () -> log.infof("участник %s офлайн"  , participantId)
            );
        }
    }


    public List<Message> getHistory(UUID conversationId , UUID userId , int limit){
        if (!participantRepository.isParticipant(conversationId , userId)){
            throw new NotParticipantException("Вы не участник этого чата");
        }
        return messageRepository.findRecent(conversationId , limit);
    }


}