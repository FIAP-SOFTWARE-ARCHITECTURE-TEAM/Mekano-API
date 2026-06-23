package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.port.in.ClienteServicePort;
import com.fiap.mekano.rest.api.dto.ClientePageResponse;
import com.fiap.mekano.rest.api.dto.ClienteResponse;
import com.fiap.mekano.rest.api.dto.CreateClienteRequest;
import com.fiap.mekano.rest.api.dto.UpdateClienteRequest;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import com.fiap.mekano.rest.api.mapper.ClienteDtoMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

@Path("/clientes")
@RequestScoped
@RolesAllowed({"admin", "atendente"})
@Tag(name = "Clientes", description = "Gerenciamento de clientes")
public class ClienteResource {

    @Inject
    ClienteServicePort clienteServicePort;

    @Inject
    ClienteDtoMapper clienteDtoMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Criar novo cliente")
    @APIResponse(responseCode = "201", description = "Cliente criado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ClienteResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados invalidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "CPF ja cadastrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response create(@Valid CreateClienteRequest request, @Context UriInfo uriInfo) {
        var command = clienteDtoMapper.toCommand(request);
        var cliente = clienteServicePort.execute(command);
        ClienteResponse response = clienteDtoMapper.toResponse(cliente);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Atualizar cliente")
    @APIResponse(responseCode = "200", description = "Cliente atualizado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ClienteResponse.class)))
    @APIResponse(responseCode = "404", description = "Cliente nao encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response update(@PathParam("id") UUID id, @Valid UpdateClienteRequest request) {
        var command = clienteDtoMapper.toCommand(request);
        var cliente = clienteServicePort.update(id, command);
        ClienteResponse response = clienteDtoMapper.toResponse(cliente);
        return Response.ok(response).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar clientes")
    @APIResponse(responseCode = "200", description = "Lista paginada de clientes",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ClientePageResponse.class)))
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("nome,asc") String sort) {
        var content = clienteServicePort.findAll(page, size, sort)
                .stream()
                .map(clienteDtoMapper::toResponse)
                .toList();
        long total = clienteServicePort.countAll();
        int totalPages = (int) Math.ceil((double) total / size);
        var response = new ClientePageResponse(content, page, size, total, totalPages);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar cliente por ID")
    @APIResponse(responseCode = "200", description = "Cliente encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ClienteResponse.class)))
    @APIResponse(responseCode = "404", description = "Cliente nao encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response getById(@PathParam("id") UUID id) {
        var cliente = clienteServicePort.findById(id);
        ClienteResponse response = clienteDtoMapper.toResponse(cliente);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir cliente (soft delete)")
    @APIResponse(responseCode = "204", description = "Cliente excluido com sucesso")
    @APIResponse(responseCode = "404", description = "Cliente nao encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response delete(@PathParam("id") UUID id) {
        clienteServicePort.delete(id);
        return Response.noContent().build();
    }
}
