package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.application.service.requisicao.RequisicaoCompraService;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.rest.api.dto.CreateRequisicaoCompraRequest;
import com.fiap.mekano.rest.api.dto.PecaResumidaResponse;
import com.fiap.mekano.rest.api.dto.RequisicaoCompraPageResponse;
import com.fiap.mekano.rest.api.dto.RequisicaoCompraResponse;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import com.fiap.mekano.rest.api.mapper.RequisicaoCompraDtoMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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

@Path("/requisicoes-compra")
@RequestScoped
@RolesAllowed("admin")
@Tag(name = "Requisições de Compra", description = "Gerenciamento de requisições de compra de insumos")
public class RequisicaoCompraResource {

    @Inject
    RequisicaoCompraService requisicaoService;

    @Inject
    PecaService pecaService;

    @Inject
    RequisicaoCompraDtoMapper requisicaoDtoMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Criar requisição de compra", description = "Cria uma nova requisição de compra (status ABERTA).")
    @APIResponse(responseCode = "201", description = "Requisição criada com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RequisicaoCompraResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response create(@Valid CreateRequisicaoCompraRequest request, @Context UriInfo uriInfo) {
        var command = requisicaoDtoMapper.toCreateCommand(request);
        var result = requisicaoService.criar(command);
        URI location = uriInfo.getAbsolutePathBuilder().path(result.id().toString()).build();
        var pecaInfo = lookupPeca(result.pecaId());
        var response = new RequisicaoCompraResponse(
                result.id(), pecaInfo, result.quantidade(),
                result.status(), result.motivo(), result.createdAt());
        return Response.created(location).entity(response).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar requisição por ID", description = "Retorna os dados da requisição de compra.")
    @APIResponse(responseCode = "200", description = "Requisição encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RequisicaoCompraResponse.class)))
    @APIResponse(responseCode = "404", description = "Requisição não encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response getById(@PathParam("id") UUID id) {
        var requisicao = requisicaoService.buscarPorId(id);
        return Response.ok(toResponse(requisicao)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar requisições", description = "Retorna requisições ativas de forma paginada")
    @APIResponse(responseCode = "200", description = "Lista paginada de requisições",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RequisicaoCompraPageResponse.class)))
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);

        var content = requisicaoService.findAll(normalizedPage, normalizedSize)
                .stream()
                .map(this::toResponse)
                .toList();
        long total = requisicaoService.countAll();
        int totalPages = (int) Math.ceil((double) total / normalizedSize);
        var response = new RequisicaoCompraPageResponse(content, normalizedPage, normalizedSize, total, totalPages);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{id}/cancelar")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cancelar requisição", description = "Cancela uma requisição de compra com status ABERTA.")
    @APIResponse(responseCode = "200", description = "Requisição cancelada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RequisicaoCompraResponse.class)))
    @APIResponse(responseCode = "404", description = "Requisição não encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Requisição não pode ser cancelada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response cancelar(@PathParam("id") UUID id) {
        requisicaoService.cancelar(id);
        var requisicao = requisicaoService.buscarPorId(id);
        return Response.ok(toResponse(requisicao)).build();
    }

    @PUT
    @Path("/{id}/enviar")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Enviar requisição", description = "Envia a requisição ao fornecedor (ABERTA → ENVIADA).")
    @APIResponse(responseCode = "200", description = "Requisição enviada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RequisicaoCompraResponse.class)))
    @APIResponse(responseCode = "404", description = "Requisição não encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Requisição não pode ser enviada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response enviar(@PathParam("id") UUID id) {
        requisicaoService.enviar(id);
        var requisicao = requisicaoService.buscarPorId(id);
        return Response.ok(toResponse(requisicao)).build();
    }

    @PUT
    @Path("/{id}/receber")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Receber requisição", description = "Registra o recebimento (ENVIADA/COMPRADA → RECEBIDA).")
    @APIResponse(responseCode = "200", description = "Requisição recebida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RequisicaoCompraResponse.class)))
    @APIResponse(responseCode = "404", description = "Requisição não encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Requisição não pode ser recebida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response receber(@PathParam("id") UUID id) {
        requisicaoService.marcarComoRecebida(id);
        var requisicao = requisicaoService.buscarPorId(id);
        return Response.ok(toResponse(requisicao)).build();
    }

    private RequisicaoCompraResponse toResponse(com.fiap.mekano.domain.model.RequisicaoCompra requisicao) {
        var pecaInfo = lookupPeca(requisicao.getPecaId());
        return new RequisicaoCompraResponse(
                requisicao.getId(), pecaInfo, requisicao.getQuantidade(),
                requisicao.getStatus().name(), requisicao.getMotivo().name(),
                requisicao.getCreatedAt());
    }

    private PecaResumidaResponse lookupPeca(UUID pecaId) {
        try {
            var peca = pecaService.buscarPorId(pecaId);
            return new PecaResumidaResponse(peca.getId(), peca.getCodigo(), peca.getDescricao());
        } catch (AppException e) {
            return null;
        }
    }

    private static int normalizeSize(int size) {
        if (size <= 0) return 10;
        return Math.min(size, 100);
    }
}
