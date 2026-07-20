package ara.service;

import ara.dto.Conversation;
import ara.dto.UserConversation;
import ara.exeption.UserNotFoundException;
import ara.repository.ConversationRepository;
import ara.repository.UserProfileRepository;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class ConversationService {



    private final ConversationRepository conversationRepository;
    private final UserProfileRepository userProfileRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               UserProfileRepository userProfileRepository) {
        this.conversationRepository = conversationRepository;
        this.userProfileRepository = userProfileRepository;
    }

    // Личный диалог находит существующий или создаёт. id детерминирован то есть  дублей не будет
    public Conversation getOrCreateDirect(UUID currentUserId, String targetUsername) {
        UUID targetId = userProfileRepository.findUserIdByUsername(targetUsername)
                .orElseThrow(() -> new UserNotFoundException("Пользователь " + targetUsername + " не найден"));

        if (targetId.equals(currentUserId)) {
            throw new IllegalArgumentException("Нельзя создать диалог с самим собой");
        }

        UUID conversationId = directConversationId(currentUserId, targetId);

        Optional<Conversation> existing = conversationRepository.findById(conversationId);
        if (existing.isPresent()) {
            return existing.get();   // диалог уже был  просто возвращаем
        }

        Instant now = Instant.now();
        Conversation c = new Conversation(conversationId, "DIRECT", null, currentUserId, now);
        conversationRepository.createConversation(c);

        conversationRepository.addParticipant(conversationId, currentUserId, "MEMBER");
        conversationRepository.addParticipant(conversationId, targetId, "MEMBER");

        // у каждого в списке чатов  свой заголовок то есть  имя собеседника
        String myName = userProfileRepository.findUsername(currentUserId).orElse("unknown");
        conversationRepository.addToUserList(currentUserId, conversationId, "DIRECT", targetUsername, now);
        conversationRepository.addToUserList(targetId, conversationId, "DIRECT", myName, now);

        log.info("Создан DIRECT %s между {} и {}", conversationId, currentUserId, targetId);
        return c;
    }

    // Группа или канал. Создатель  OWNER
    public Conversation createGroup(UUID creatorId, String title, String type, List<String> memberUsernames) {
        if (!type.equals("GROUP") && !type.equals("CHANNEL")) {
            throw new IllegalArgumentException("Тип должен быть GROUP или CHANNEL");
        }

        UUID conversationId = Uuids.timeBased();   // UUIDv1  сортировка  по времени
        Instant now = Instant.now();

        Conversation c = new Conversation(conversationId, type, title, creatorId, now);
        conversationRepository.createConversation(c);

        conversationRepository.addParticipant(conversationId, creatorId, "OWNER");
        conversationRepository.addToUserList(creatorId, conversationId, type, title, now);

        for (String username : memberUsernames) {
            userProfileRepository.findUserIdByUsername(username).ifPresentOrElse(memberId -> {
                conversationRepository.addParticipant(conversationId, memberId, "MEMBER");
                conversationRepository.addToUserList(memberId, conversationId, type, title, now);
            }, () -> log.warn("Пользователь {} не найден, пропускаем", username));
        }

        log.info("Создан {} {} {} пользователем {}", type, conversationId, title, creatorId);
        return c;
    }

    public List<UserConversation> listConversations(UUID userId) {
        return conversationRepository.findUserConversations(userId);
    }

    // Один и тот же id для пары (A,B) независимо от порядка
    private UUID directConversationId(UUID a, UUID b) {
        UUID first  = a.compareTo(b) <= 0 ? a : b;
        UUID second = a.compareTo(b) <= 0 ? b : a;
        return UUID.nameUUIDFromBytes((first + ":" + second).getBytes(StandardCharsets.UTF_8));
    }
}