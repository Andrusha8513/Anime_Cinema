package ara.controller;


import ara.dto.CompleteRegistrationRequest;
import ara.dto.LoginRequestEmail;
import ara.dto.LogoutRequest;
import ara.dto.RefreshRequest;
import ara.entity.RefreshToken;
import ara.jwt.JwtAuthenticationDto;
import ara.service.AuthService;
import ara.utiliti.ClientInfoExtractor;
import ara.utiliti.DeviceInfoParser;
import io.quarkus.security.Authenticated;
import io.vertx.core.http.HttpServerRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;


import java.util.List;
import java.util.UUID;

@Slf4j
@Path("/auth")
public class AuthController {

    private final AuthService authService;
    private final JsonWebToken jwt;

    public AuthController(AuthService authService, JsonWebToken jwt) {
        this.authService = authService;
        this.jwt = jwt;
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Valid LoginRequestEmail loginRequestEmail , @Context HttpServerRequest request ,
                          @HeaderParam("User-Agent") String userAgent){
       String ip = ClientInfoExtractor.extractClientIp(request);
       String device = DeviceInfoParser.parseDeviceInfo(userAgent);

        JwtAuthenticationDto tokens = authService.login(loginRequestEmail.email() , loginRequestEmail.password() , device , ip);
        return Response.ok(tokens).build();
    }

    @POST
    @Path("/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh(@Valid RefreshRequest request){
       JwtAuthenticationDto tokens = authService.refresh(request.refreshToken());
       return Response.ok(tokens).build();
    }

//    @POST
//    @Path("/set-password")
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response setPasswordRequest(@Valid SetPasswordRequest request){
//        authService.setPasswordRequest(request);
//        return Response.ok().build();
//    }

    @POST
    @Path("/logout")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(@Valid LogoutRequest request){
        authService.logout(request);
        return Response.ok().build();
    }

    @POST
    @Path("/all-logout")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logoutFromAll(){
        UUID userId = UUID.fromString(jwt.getSubject());
        authService.logoutFromAllDevices(userId);
        return Response.ok().build();
    }

    @GET
    @Path("/sessions")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveSessions(){
        UUID userId = UUID.fromString(jwt.getSubject());
        List<RefreshToken> session = authService.getActiveSessions(userId);
        return Response.ok(session).build();
    }

    @POST
    @Path("/complete-registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response completeRegistration(@Valid CompleteRegistrationRequest request ,
                                         @Context HttpServerRequest httpRequest ,
                                         @HeaderParam("User-Agent") String userAgent){
        String ip = ClientInfoExtractor.extractClientIp(httpRequest);
        String device = DeviceInfoParser.parseDeviceInfo(userAgent);

        JwtAuthenticationDto tokens = authService.completeRegistration(request.token() ,request.password() , device , ip);

        return Response.ok(tokens).build();
    }

}
