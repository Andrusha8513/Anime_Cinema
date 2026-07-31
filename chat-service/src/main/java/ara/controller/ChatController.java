package ara.controller;

import ara.dto.MessageCursor;
import ara.dto.MessagePage;
import ara.dto.MessagePageResponse;
import ara.jwt.ChatTokenVerifier;
import ara.dto.MessageResponse;

import ara.service.ChatMessageService;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/chat")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final ChatTokenVerifier verifier;

    public ChatController(ChatMessageService chatMessageService, ChatTokenVerifier verifier) {
        this.chatMessageService = chatMessageService;
        this.verifier = verifier;
    }




    @GET
    @Path("/messages/{conversationId}")
    @Blocking
    @Produces(MediaType.APPLICATION_JSON)
    public Response history(@HeaderParam("Authorization") String authHeader,
                            @PathParam("conversationId") UUID conversationId,
                            @QueryParam("beforeBucket") Integer beforeBucket,
                            @QueryParam("beforeMessageId") UUID beforeMessageId,
                            @QueryParam("limit") @DefaultValue("50") int limit) {

        UUID userId = verifier.verifyBearerHeader(authHeader);

        MessageCursor cursor = (beforeBucket != null && beforeMessageId != null)
                ? new MessageCursor(beforeBucket, beforeMessageId)
                : null;

        MessagePage page = chatMessageService.getHistory(conversationId, userId, cursor, limit);
        return Response.ok(MessagePageResponse.from(page)).build();
    }
}

