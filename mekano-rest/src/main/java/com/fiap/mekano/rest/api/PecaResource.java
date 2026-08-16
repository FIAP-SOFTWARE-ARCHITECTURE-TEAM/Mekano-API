package com.fiap.mekano.rest.api;

import java.net.URI;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.domain.port.in.UpdatePecaCommand;
import com.fiap.mekano.rest.api.dto.CreatePecaRequest;
import com.fiap.mekano.rest.api.dto.PecaPageResponse;
import com.fiap.mekano.rest.api.dto.PecaResponse;
import com.fiap.mekano.rest.api.dto.UpdatePecaRequest;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import com.fiap.mekano.rest.api.mapper.PecaDtoMapper;

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

@Path("/pecas")
@RequestScoped
@RolesAllowed("admin")
@Tag(name = "Peças", description = "Gerenciamento de peças e insumos do estoque")
public class PecaResource {

        @Inject
        PecaService pecaService;

        @Inject
        PecaDtoMapper pecaDtoMapper;

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Cadastrar nova peça", description = "Cadastra uma nova peça no estoque.")
        @APIResponse(responseCode = "201", description = "Peça cadastrada com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaResponse.class)))
        @APIResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        public Response create(@Valid CreatePecaRequest request, @Context UriInfo uriInfo) {
                var command = pecaDtoMapper.toCreateCommand(request);
                var response = pecaService.criar(command);
                URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
                var pecaResponse = new PecaResponse(
                                response.id(), response.codigo(), response.descricao(),
                                response.valorUnitario(),
                                response.saldoAtual(), response.estoqueMinimo(), response.createdAt());
                return Response.created(location).entity(pecaResponse).build();
        }

        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Buscar peça por ID", description = "Retorna os dados da peça ativa.")
        @APIResponse(responseCode = "200", description = "Peça encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaResponse.class)))
        @APIResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        public Response getById(@PathParam("id") UUID id) {
                var peca = pecaService.buscarPorId(id);
                PecaResponse response = pecaDtoMapper.toResponse(peca);
                return Response.ok(response).build();
        }

        @PUT
        @Path("/{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Atualizar peça", description = "Atualiza dados cadastrais de uma peça. Saldo atual e saldo reservado não são alteráveis via API.")
        @APIResponse(responseCode = "200", description = "Peça atualizada com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaResponse.class)))
        @APIResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        @APIResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        public Response atualizar(@PathParam("id") UUID id, @Valid UpdatePecaRequest request) {
                UpdatePecaCommand command = pecaDtoMapper.toUpdateCommand(id, request);
                var peca = pecaService.updatePeca(id, command);
                PecaResponse response = pecaDtoMapper.toResponse(peca);
                return Response.ok(response).build();
        }

        @DELETE
        @Path("/{id}")
        @Operation(summary = "Excluir peça", description = "Marca a peça como inativa (soft delete). Retorna 409 se a peça estiver vinculada a uma OS ativa.")
        @APIResponse(responseCode = "204", description = "Peça excluída com sucesso")
        @APIResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        @APIResponse(responseCode = "409", description = "Peça vinculada a OS ativa", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        public Response excluir(@PathParam("id") UUID id) {
                pecaService.excluir(id);
                return Response.noContent().build();
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Listar peças", description = "Retorna peças ativas de forma paginada")
        @APIResponse(responseCode = "200", description = "Lista paginada de peças", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaPageResponse.class)))
        public Response listAll(
                        @QueryParam("page") @DefaultValue("0") int page,
                        @QueryParam("size") @DefaultValue("10") int size) {
                int normalizedPage = Math.max(page, 0);
                int normalizedSize = normalizeSize(size);

                var content = pecaService.findAll(normalizedPage, normalizedSize)
                                .stream()
                                .map(pecaDtoMapper::toResponse)
                                .toList();
                long total = pecaService.countAll();
                int totalPages = (int) Math.ceil((double) total / normalizedSize);
                var response = new PecaPageResponse(content, normalizedPage, normalizedSize, total, totalPages);
                return Response.ok(response).build();
        }

        private static int normalizeSize(int size) {
                if (size <= 0)
                        return 10;
                return Math.min(size, 100);
        }
}
