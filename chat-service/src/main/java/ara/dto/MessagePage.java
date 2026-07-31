package ara.dto;
import java.util.List;

public record MessagePage(
        List<Message> messages,
        MessageCursor nextCursor,
        boolean hasMore
) {}