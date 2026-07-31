package ara.dto;

import java.util.UUID;

public record ConversationSummary(UUID conversationId, String type, String title) {}