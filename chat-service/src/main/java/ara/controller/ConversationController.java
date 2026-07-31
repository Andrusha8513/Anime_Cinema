package ara.controller;

import ara.dto.Conversation;
import ara.dto.ConversationResponse;
import ara.dto.CreateDirectRequest;
import ara.dto.CreateGroupRequest;
import ara.jwt.ChatTokenVerifier;
import ara.service.ConversationService;
import io.smallrye.common.annotation.Blocking;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/chat/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatTokenVerifier verifier;

    public ConversationController(ConversationService conversationService, ChatTokenVerifier verifier) {
        this.conversationService = conversationService;
        this.verifier = verifier;
    }

    // Написать человеку по username найдёт или создаст диалог
    @POST
    @Path("/direct")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDirect(@HeaderParam("Authorization") String auth,
                                 @Valid CreateDirectRequest request) {
        UUID userId = verifier.verifyBearerHeader(auth);
        Conversation c = conversationService.getOrCreateDirect(userId, request.username());
        return Response.ok(ConversationResponse.from(c)).build();
    }

    // Создать группу или канал
    @POST
    @Path("/group")
    @Blocking
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(@HeaderParam("Authorization") String auth,
                                @Valid CreateGroupRequest request) {
        UUID userId = verifier.verifyBearerHeader(auth);
        Conversation c = conversationService.createGroup(
                userId, request.title(), request.type(), request.members());
        return Response.status(Response.Status.CREATED)
                .entity(ConversationResponse.from(c)).build();
    }

    // Список моих чатов, свежие сверху
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@HeaderParam("Authorization") String auth) {
        UUID userId = verifier.verifyBearerHeader(auth);
        return Response.ok(conversationService.listConversations(userId)).build();
    }
}