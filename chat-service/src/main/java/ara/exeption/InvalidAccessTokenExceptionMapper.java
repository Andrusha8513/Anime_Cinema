package ara.exeption;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidAccessTokenExceptionMapper implements ExceptionMapper<InvalidAccessTokenException> {
    @Override
    public Response toResponse(InvalidAccessTokenException e) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    }
}
