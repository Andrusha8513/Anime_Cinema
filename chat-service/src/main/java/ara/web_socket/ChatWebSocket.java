package ara.web_socket;

import ara.dto.IncomingWsMessage;
import ara.dto.OutgoingWsMessage;
import ara.service.ChatMessageService;
import io.quarkus.websockets.next.*;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;


@WebSocket(path = "/ws/chat")
public class ChatWebSocket {

    private static final Logger log = Logger.getLogger(ChatWebSocket.class);


    private final ChatMessageService chatMessageService;
    private final ConnectionRegistry registry;
    private final WebSocketConnection connection;

    public ChatWebSocket(ChatMessageService chatMessageService,
                         ConnectionRegistry registry,
                         WebSocketConnection connection) {
        this.chatMessageService = chatMessageService;
        this.registry = registry;
        this.connection = connection;
    }

    @OnOpen
    public void onOpen() {
        log.debugf("Соединение открыто: %s (ожидание auth)", connection.id());
    }

    @OnTextMessage
    @Blocking
    public void onMessage(IncomingWsMessage incoming) {
        if (incoming.type() == null) {
            connection.sendTextAndAwait(OutgoingWsMessage.error("Поле type обязательно"));
            return;
        }
        try {
            switch (incoming.type()) {
                case "auth" -> chatMessageService.authenticate(connection, incoming);
                case "message" -> chatMessageService.handleIncoming(connection, incoming);
                default -> connection.sendTextAndAwait(
                        OutgoingWsMessage.error("Неизвестный тип: " + incoming.type()));
            }
        } catch (Exception e) {
            log.error("Ошибка обработки сообщения от {}", connection.id(), e);
            connection.sendTextAndAwait(OutgoingWsMessage.error(e.getMessage()));
        }
    }

    @OnClose
    public void onClose() {
        registry.unregister(connection.id());
    }

    @OnError
    public void onError(Throwable t) {
        log.error("Ошибка в сокете {}", connection.id() , t);
    }
}