package ara.controller;

import ara.jwt.ChatTokenVerifier;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/chat/verify-test")
public class VerifyTestController {
    private final ChatTokenVerifier verifier;
    public VerifyTestController(ChatTokenVerifier verifier) { this.verifier = verifier; }

    @GET
    public Response test(@HeaderParam("Authorization") String auth) {
        String token = auth.substring("Bearer ".length());
        UUID userId = verifier.verifyAndExtractUserId(token);
        return Response.ok("userId = " + userId).build();
    }
}