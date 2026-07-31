package ara.service;

import ara.dto.Conversation;
import ara.dto.ConversationSummary;
import ara.enums.ConversationType;
import ara.enums.ParticipantRole;
import ara.exeption.UserNotFoundException;
import ara.repository.ConversationRepository;
import ara.repository.ParticipantRepository;
import ara.repository.UserProfileRepository;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationService {

    private static final Logger log = Logger.getLogger(ConversationService.class);

    private static final int MAX_GROUP_MEMBERS = 500;

    private final ConversationRepository conversationRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParticipantRepository participantRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               UserProfileRepository userProfileRepository,
                               ParticipantRepository participantRepository) {
        this.conversationRepository = conversationRepository;
        this.userProfileRepository = userProfileRepository;
        this.participantRepository = participantRepository;
    }


    public Conversation getOrCreateDirect(UUID currentUserId, String targetUsername) {
        UUID targetId = resolveUser(targetUsername);
        validateNotSelf(currentUserId, targetId);

        UUID conversationId = directConversationId(currentUserId, targetId);
        return conversationRepository.findById(conversationId)
                .orElseGet(() -> createDirect(conversationId, currentUserId, targetId));
    }


    public Conversation createGroup(UUID creatorId, String title, ConversationType type,
                                    List<String> memberUsernames) {
        if (type != ConversationType.GROUP && type != ConversationType.CHANNEL) {
            throw new IllegalArgumentException("Тип должен быть GROUP или CHANNEL");
        }
        if (memberUsernames != null && memberUsernames.size() > MAX_GROUP_MEMBERS) {
            throw new IllegalArgumentException("Слишком много участников (макс " + MAX_GROUP_MEMBERS + ")");
        }

        UUID conversationId = Uuids.timeBased();
        Instant now = Instant.now();


        Conversation c = new Conversation(conversationId, type.name(), title, creatorId, now);
        conversationRepository.createConversation(c);

        addMember(conversationId, creatorId, ParticipantRole.OWNER, type);

        if (memberUsernames != null) {
            Map<String, UUID> userIdsMap = userProfileRepository.findUserIdsByUsernames(memberUsernames);
            for (String username : memberUsernames) {
                UUID memberId = userIdsMap.get(username);
                if (memberId != null) {
                    addMember(conversationId, memberId, ParticipantRole.MEMBER, type);
                } else {
                    log.warnf("Пользователь %s не найден, пропускаем", username);
                }
            }
        }

        log.infof("Создан %s %s '%s' пользователем %s", type, conversationId, title, creatorId);
        return c;
    }

    public List<ConversationSummary> listConversations(UUID userId) {
        return conversationRepository.findUserConversationIds(userId).stream()
                .map(conversationRepository::findById)
                .flatMap(Optional::stream)
                .map(c -> toSummary(c, userId))
                .toList();
    }


    private ConversationSummary toSummary(Conversation c, UUID currentUserId) {
        String displayName;
        if (ConversationType.DIRECT.name().equals(c.type())) {
            UUID otherId = participantRepository.findUserIds(c.conversationId()).stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst().orElse(null);
            displayName = (otherId != null)
                    ? userProfileRepository.findUsername(otherId).orElse("Пользователь")
                    : "Диалог";
        } else {
            displayName = c.title();
        }
        return new ConversationSummary(c.conversationId(), c.type(), displayName);
    }

    private Conversation createDirect(UUID conversationId, UUID currentUserId, UUID targetId) {
        Instant now = Instant.now();


        Conversation c = new Conversation(conversationId, ConversationType.DIRECT.name(),
                null, currentUserId, now);
        conversationRepository.createConversation(c);

        addMember(conversationId, currentUserId, ParticipantRole.MEMBER, ConversationType.DIRECT);
        addMember(conversationId, targetId, ParticipantRole.MEMBER, ConversationType.DIRECT);

        log.infof("Создан DIRECT %s между %s и %s", conversationId, currentUserId, targetId);
        return c;
    }


    private void addMember(UUID conversationId, UUID userId, ParticipantRole role, ConversationType type) {
        conversationRepository.addParticipant(conversationId, userId, role.name());
        conversationRepository.addToUserList(userId, conversationId, type.name());
    }

    private UUID resolveUser(String username) {
        return userProfileRepository.findUserIdByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь " + username + " не найден"));
    }

    private void validateNotSelf(UUID currentUserId, UUID targetId) {
        if (targetId.equals(currentUserId)) {
            throw new IllegalArgumentException("Нельзя создать диалог с самим собой");
        }
    }


    private UUID directConversationId(UUID a, UUID b) {
        UUID first  = a.compareTo(b) <= 0 ? a : b;
        UUID second = a.compareTo(b) <= 0 ? b : a;
        return UUID.nameUUIDFromBytes((first + ":" + second).getBytes(StandardCharsets.UTF_8));
    }
}