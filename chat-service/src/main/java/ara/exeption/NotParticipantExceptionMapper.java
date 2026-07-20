package ara.exeption;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotParticipantExceptionMapper implements ExceptionMapper<NotParticipantException> {
    @Override
    public Response toResponse(NotParticipantException e) {
        return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
    }
}
