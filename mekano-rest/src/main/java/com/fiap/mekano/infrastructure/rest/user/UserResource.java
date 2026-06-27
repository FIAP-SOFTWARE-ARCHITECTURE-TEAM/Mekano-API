package com.fiap.mekano.infrastructure.rest.user;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/users")
@RolesAllowed({"admin"})
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public String listUsers() {
        return "somente admin";
    }
}
