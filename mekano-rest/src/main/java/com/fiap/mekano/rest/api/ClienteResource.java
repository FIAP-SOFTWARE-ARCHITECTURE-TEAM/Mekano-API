package com.fiap.mekano.rest.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.in.ClienteServicePort;
import com.fiap.mekano.domain.port.in.CreateClienteCommand;
import com.fiap.mekano.domain.port.in.UpdateClienteCommand;
import com.fiap.mekano.rest.api.dto.ClientePageResponse;
import com.fiap.mekano.rest.api.dto.ClienteResponse;
import com.fiap.mekano.rest.api.dto.CreateClienteRequest;
import com.fiap.mekano.rest.api.dto.UpdateClienteRequest;
import com.fiap.mekano.rest.api.mapper.ClienteDtoMapper;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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

@Path("/clientes")
@RequestScoped
@RolesAllowed({"admin", "atendente"})
@Tag(name = "Clientes", description = "Gerenciamento de clientes")
public class ClienteResource {

    @Inject
    private ClienteServicePort clienteService;

    @Inject
    private ClienteDtoMapper clienteDtoMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cadastrar cliente", description = "Cadastra um novo cliente no sistema.")
    public Response create(
            @Valid CreateClienteRequest request,
            @Context UriInfo uriInfo) {

        CreateClienteCommand command = clienteDtoMapper.toCommand(request);

        Cliente cliente = clienteService.execute(command);

        ClienteResponse response = clienteDtoMapper.toResponse(cliente);

        URI location = uriInfo
                .getAbsolutePathBuilder()
                .path(response.id().toString())
                .build();

        return Response
                .created(location)
                .entity(response)
                .build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Atualizar cliente", description = "Atualiza os dados de um cliente existente.")
    public Response update(
            @PathParam("id") UUID id,
            @Valid UpdateClienteRequest request) {

        UpdateClienteCommand command = clienteDtoMapper.toCommand(request);

        Cliente cliente = clienteService.updateCliente(id, command);

        return Response.ok(
                clienteDtoMapper.toResponse(cliente)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar clientes", description = "Lista todos os clientes cadastrados, com paginação e ordenação.")
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("nome,asc") String sort,
            @QueryParam("isActive") Boolean isActive) {

        List<ClienteResponse> content = clienteService.findAllClientes(page, size, sort, isActive)
                .stream()
                .map(clienteDtoMapper::toResponse)
                .toList();

        long total = clienteService.countAllClientes(isActive);

        int totalPages = (int) Math.ceil((double) total / size);

        ClientePageResponse response = new ClientePageResponse(
                content,
                page,
                size,
                total,
                totalPages);

        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar cliente por ID", description = "Retorna os dados de um cliente específico com base no ID informado.")
    public Response findById(
            @PathParam("id") UUID id) {

        Cliente cliente = clienteService.findClienteById(id);

        return Response.ok(
                clienteDtoMapper.toResponse(cliente)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remover cliente", description = "Remove logicamente um cliente existente com base no ID informado.")
    public Response delete(
            @PathParam("id") UUID id) {

        clienteService.deleteCliente(id);

        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/ativar")
    @Operation(summary = "Reativar cliente", description = "Reativa um cliente inativo. Se o cliente já estiver ativo, nenhuma alteração é feita.")
    public Response reativar(
            @PathParam("id") UUID id) {

        clienteService.reactivate(id);

        return Response.noContent().build();
    }
}
