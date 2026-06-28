package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.port.in.AdminUserServicePort;
import com.fiap.mekano.domain.port.in.CreateAdminUserCommand;
import com.fiap.mekano.rest.dto.admin.AdminCreateUserRequest;
import com.fiap.mekano.rest.dto.admin.AdminCreateUserResponse;
import com.fiap.mekano.rest.dto.admin.AdminUserSummaryResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

@Path("/admin/usuarios")
@RolesAllowed("admin")
public class AdminUserResource {

    @Inject
    AdminUserServicePort adminUserService;

    @POST
    public Response criar(@Valid AdminCreateUserRequest request) {
        var result = adminUserService.criarUsuario(
                new CreateAdminUserCommand(
                        request.name(),
                        request.email(),
                        request.role()
                )
        );

        return Response
                .created(URI.create("/admin/usuarios/" + result.id()))
                .entity(AdminCreateUserResponse.from(result))
                .build();
    }

    @GET
    public Response listar(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        var users = adminUserService.listar(page, size)
                .stream()
                .map(AdminUserSummaryResponse::from)
                .toList();

        return Response.ok(users).build();
    }

    @DELETE
    @Path("/{uuid}")
    public Response deletar(@PathParam("uuid") UUID uuid) {
        adminUserService.deletar(uuid);
        return Response.noContent().build();
    }
}