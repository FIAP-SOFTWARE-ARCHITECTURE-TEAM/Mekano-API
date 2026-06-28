package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.UnidadeMedida;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.entity.PecaEntity;
import com.fiap.mekano.infrastructure.repository.PecaPanacheRepository;
import com.fiap.mekano.rest.api.dto.CreatePecaRequest;
import com.fiap.mekano.rest.api.dto.PecaPageResponse;
import com.fiap.mekano.rest.api.dto.PecaResponse;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import com.fiap.mekano.rest.api.mapper.PecaDtoMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import java.time.LocalDateTime;
import java.util.UUID;

@Path("/pecas")
@RequestScoped
@RolesAllowed("admin")
@Tag(name = "Peças", description = "Gerenciamento de peças e insumos do estoque")
public class PecaResource {

    @Inject
    PecaService pecaService;

    @Inject
    PecaRepositoryPort pecaRepository;

    @Inject
    PecaPanacheRepository pecaPanacheRepository;

    @Inject
    PecaDtoMapper pecaDtoMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cadastrar nova peça", description = "Cadastra uma nova peça no estoque.")
    @APIResponse(responseCode = "201", description = "Peça cadastrada com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response create(@Valid CreatePecaRequest request, @Context UriInfo uriInfo) {
        var command = pecaDtoMapper.toCreateCommand(request);
        var response = pecaService.criar(command);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        var pecaResponse = new PecaResponse(
                response.id(), null, response.descricao(), null, null,
                (long) response.saldo(), (long) response.estoqueMinimo(), null);
        return Response.created(location).entity(pecaResponse).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar peça por ID", description = "Retorna os dados da peça ativa.")
    @APIResponse(responseCode = "200", description = "Peça encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaResponse.class)))
    @APIResponse(responseCode = "404", description = "Peça não encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response getById(@PathParam("id") UUID id) {
        var peca = pecaRepository.buscarPorId(id)
                .orElseThrow(() -> new com.fiap.mekano.domain.exception.AppException(404,
                        com.fiap.mekano.domain.exception.Messages.get("peca.not.found", id)));
        PecaResponse response = pecaDtoMapper.toResponse(peca);
        return Response.ok(response).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar peças", description = "Retorna peças ativas de forma paginada")
    @APIResponse(responseCode = "200", description = "Lista paginada de peças",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaPageResponse.class)))
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);

        var query = pecaPanacheRepository.find("isActive = ?1", io.quarkus.panache.common.Sort.by("id"), true);
        var panachePage = query.page(io.quarkus.panache.common.Page.of(normalizedPage, normalizedSize));
        var entities = panachePage.list();
        var content = entities.stream()
                .map(this::toDomain)
                .map(pecaDtoMapper::toResponse)
                .toList();
        long total = pecaPanacheRepository.count("isActive", true);
        int totalPages = (int) Math.ceil((double) total / normalizedSize);
        var response = new PecaPageResponse(content, normalizedPage, normalizedSize, total, totalPages);
        return Response.ok(response).build();
    }

    private static int normalizeSize(int size) {
        if (size <= 0) return 10;
        return Math.min(size, 100);
    }

    private Peca toDomain(PecaEntity entity) {
        return Peca.reconstitute(
                entity.uuid,
                entity.uuid.toString(),
                entity.descricao,
                UnidadeMedida.UNIDADE,
                java.math.BigDecimal.ZERO,
                entity.saldo == null ? 0L : entity.saldo.longValue(),
                entity.estoqueMinimo == null ? 0L : entity.estoqueMinimo.longValue(),
                entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt()
        );
    }
}
