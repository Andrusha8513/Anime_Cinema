package ara.controller;

import ara.jwt.ChatTokenVerifier;
import ara.repository.MessageRepository;
import ara.dto.MessageResponse;
import ara.dto.SendMessageRequest;

import ara.dto.Message;
import ara.web_socket.ChatMessageService;
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response recept(@HeaderParam("Authorization") String authHeader,
                           @PathParam("conversationId") UUID conversationId ,
                           @QueryParam("limit") @DefaultValue("50") int limit){
        UUID userId = verifier.verifyBearerHeader(authHeader);
        List<MessageResponse> responses = chatMessageService.getHistory(conversationId , userId , limit)
                .stream()
                .map(MessageResponse::from).toList();
        return Response.ok(responses).build();
    }
}

