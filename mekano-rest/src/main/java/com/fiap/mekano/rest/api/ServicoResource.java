package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.port.in.ServicoServicePort;
import com.fiap.mekano.rest.api.dto.CreateServicoRequest;
import com.fiap.mekano.rest.api.dto.ServicoPageResponse;
import com.fiap.mekano.rest.api.dto.ServicoResponse;
import com.fiap.mekano.rest.api.dto.UpdateServicoRequest;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import com.fiap.mekano.rest.api.mapper.ServicoDtoMapper;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

/**
 * JAX-RS Resource para operações CRUD de serviço.
 *
 * <p>Todos os endpoints são {@code @RolesAllowed("admin")} — acesso exclusivo administrativo.
 * {@code @RequestScoped} necessário para {@code @Context UriInfo} funcionar (G8).
 */
@Path("/servicos")
@RequestScoped
@RolesAllowed("admin")
@Tag(name = "Serviços", description = "Gerenciamento de serviços da oficina")
public class ServicoResource {

    @Inject
    ServicoServicePort servicoServicePort;

    @Inject
    ServicoDtoMapper servicoDtoMapper;

    /**
     * Cria um novo serviço.
     *
     * @param request DTO validado por Bean Validation
     * @param uriInfo contexto para construção da URI Location
     * @return 201 Created com ServicoResponse e Location header
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Criar novo serviço", description = "Cria um novo serviço na oficina. Retorna 409 se o nome já existir.")
    @APIResponse(responseCode = "201", description = "Serviço criado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ServicoResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Nome de serviço já cadastrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response create(@Valid CreateServicoRequest request, @Context UriInfo uriInfo) {
        var command = servicoDtoMapper.toCreateCommand(request);
        var servico = servicoServicePort.create(command);
        ServicoResponse response = servicoDtoMapper.toResponse(servico);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    /**
     * Atualiza um serviço existente.
     *
     * @param id      UUID do serviço
     * @param request DTO com novos dados
     * @return 200 OK com ServicoResponse atualizado
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Atualizar serviço", description = "Atualiza nome, descrição e valor de um serviço existente.")
    @APIResponse(responseCode = "200", description = "Serviço atualizado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ServicoResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "Serviço não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Nome já utilizado por outro serviço",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response update(@PathParam("id") UUID id, @Valid UpdateServicoRequest request) {
        var command = servicoDtoMapper.toUpdateCommand(request);
        var servico = servicoServicePort.update(id, command);
        ServicoResponse response = servicoDtoMapper.toResponse(servico);
        return Response.ok(response).build();
    }

    /**
     * Busca um serviço pelo UUID.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar serviço por ID", description = "Retorna os dados do serviço ativo.")
    @APIResponse(responseCode = "200", description = "Serviço encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ServicoResponse.class)))
    @APIResponse(responseCode = "404", description = "Serviço não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response getById(@PathParam("id") UUID id) {
        var servico = servicoServicePort.findById(id);
        ServicoResponse response = servicoDtoMapper.toResponse(servico);
        return Response.ok(response).build();
    }

    /**
     * Lista todos os serviços ativos paginados.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar serviços", description = "Retorna serviços ativos de forma paginada e ordenada")
    @APIResponse(responseCode = "200", description = "Lista paginada de serviços",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ServicoPageResponse.class)))
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("nome,asc") String sort) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);
        String normalizedSort = sort == null || sort.isBlank() ? "nome,asc" : sort;

        var content = servicoServicePort.findAll(normalizedPage, normalizedSize, normalizedSort)
                .stream()
                .map(servicoDtoMapper::toResponse)
                .toList();
        long total = servicoServicePort.countAll();
        int totalPages = (int) Math.ceil((double) total / normalizedSize);
        var response = new ServicoPageResponse(content, normalizedPage, normalizedSize, total, totalPages);
        return Response.ok(response).build();
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    /**
     * Exclui (soft delete) um serviço.
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir serviço", description = "Marca o serviço como inativo (soft delete).")
    @APIResponse(responseCode = "204", description = "Serviço excluído com sucesso")
    @APIResponse(responseCode = "404", description = "Serviço não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response delete(@PathParam("id") UUID id) {
        servicoServicePort.delete(id);
        return Response.noContent().build();
    }
}
