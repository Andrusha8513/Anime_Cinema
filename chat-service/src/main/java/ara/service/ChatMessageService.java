package ara.service;

import ara.dto.*;
import ara.exeption.ConversationNotFoundException;
import ara.exeption.NotParticipantException;
import ara.jwt.ChatTokenVerifier;
import ara.repository.ConversationRepository;
import ara.repository.MessageRepository;
import ara.repository.ParticipantRepository;
import ara.repository.UserProfileRepository;
import ara.utilita.BucketUtil;
import ara.web_socket.ConnectionRegistry;
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
    private final ConversationRepository conversationRepository;
    private final UserProfileRepository userProfileRepository;

    public ChatMessageService(MessageRepository messageRepository,
                              ChatTokenVerifier verifier,
                              ConnectionRegistry registry,
                              ParticipantRepository participantRepository,
                              OpenConnections openConnections,
                              ConversationRepository conversationRepository,
                              UserProfileRepository userProfileRepository) {
        this.messageRepository = messageRepository;
        this.verifier = verifier;
        this.registry = registry;
        this.participantRepository = participantRepository;
        this.openConnections = openConnections;
        this.conversationRepository = conversationRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public void authenticate(WebSocketConnection connection, IncomingWsMessage message) {
        if (message.token() == null || message.token().isBlank()) {
            connection.sendTextAndAwait(OutgoingWsMessage.error("Токен обязателен"));
            connection.closeAndAwait();
            return;
        }

        UUID userId = verifier.verifyAndExtractUserId(message.token());
        registry.register(connection.id(), userId);
        log.infof("Пользователь %s аутентифицирован в сокете %s", userId, connection.id());
        connection.sendTextAndAwait(OutgoingWsMessage.authOk());
    }

    public void handleIncoming(WebSocketConnection connection, IncomingWsMessage message) {
        Optional<UUID> userOpt = registry.userOf(connection.id());

        if (userOpt.isEmpty()) {
            connection.sendTextAndAwait(OutgoingWsMessage.error("Сначала аутентифицируйтесь"));
            return;
        }

        UUID senderId = userOpt.get();
        UUID conversationId = message.conversationId();

        Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
        if (convOpt.isEmpty()) {
            connection.sendTextAndAwait(OutgoingWsMessage.error("Чат не найден"));
            return;
        }
        Conversation conv = convOpt.get();

        String role = participantRepository.findRole(conversationId, senderId).orElse(null);
        if (role == null) {
            connection.sendTextAndAwait(OutgoingWsMessage.error("Вы не участник этого чата"));
            return;
        }

        boolean isChannel = "CHANNEL".equals(conv.type());
        boolean canWrite = !isChannel || role.equals("OWNER") || role.equals("ADMIN");

        if (!canWrite) {
            connection.sendTextAndAwait(OutgoingWsMessage.error("В этом канале могут писать только администраторы"));
            return;
        }

        Message saved = messageRepository.save(conversationId, senderId, message.content());

        List<UUID> participants = participantRepository.findUserIds(conversationId);
        broadcast(conversationId, saved, participants);
    }

    private void broadcast(UUID conversationId, Message saved, List<UUID> participants) {
        String senderName = userProfileRepository.findUsername(saved.senderId()).orElse("Пользователь");
        OutgoingWsMessage out = OutgoingWsMessage.of(saved, senderName);

        log.infof("broadcast: чат %s, участников %s", conversationId, participants.size());
        for (UUID participantId : participants) {
            registry.connectionIdOf(participantId)
                    .flatMap(openConnections::findByConnectionId)
                    .ifPresentOrElse(
                            conn -> {
                                conn.sendTextAndAwait(out);
                                log.infof("доставлено -> %s", participantId);
                            },
                            () -> log.infof("участник %s офлайн", participantId)
                    );
        }
    }

    public MessagePage getHistory(UUID conversationId, UUID userId, MessageCursor cursor, int limit) {
        if (!participantRepository.isParticipant(conversationId, userId)) {
            throw new NotParticipantException("Вы не участник этого чата");
        }

        int minBucket = conversationRepository.findById(conversationId)
                .map(c -> BucketUtil.bucketFor(c.createdAt()))
                .orElseThrow(() -> new ConversationNotFoundException("Чат не найден: " + conversationId));

        return messageRepository.findPage(conversationId, minBucket, cursor, limit);
    }
}