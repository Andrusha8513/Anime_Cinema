package ara.dto;


import java.time.Instant;
import java.util.UUID;

public record UserConversation(UUID conversationId, String type, String title, Instant lastMessageAt) {}