package com.fiap.mekano.infrastructure.rest.auth;

import com.fiap.mekano.domain.port.in.AuthServicePort;
import com.fiap.mekano.domain.port.in.LoginCommand;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RequestScoped
@Path("/auth")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthServicePort authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        var tokenPair = authService.login(
                new LoginCommand(request.email(), request.password())
        );

        return Response.ok(TokenResponse.from(tokenPair)).build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(RefreshRequest request) {
        var tokenPair = authService.refresh(request.refreshToken());

        return Response.ok(TokenResponse.from(tokenPair)).build();
    }

    @POST
    @Path("/logout")
    public Response logout(LogoutRequest request) {
        authService.logout(request.refreshToken());

        return Response.noContent().build();
    }
}
