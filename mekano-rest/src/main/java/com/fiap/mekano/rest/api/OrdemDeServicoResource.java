package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.rest.api.dto.CreateOrdemDeServicoRequest;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoPageResponse;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoResponse;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoStatusResponse;
import jakarta.annotation.security.PermitAll;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

/**
 * Resource para Ordens de Serviço.
 *
 * Roles mistas (D-14, D-15):
 * - POST: admin, atendente
 * - PUT transições: mecanico, admin
 * - GET /status: @PermitAll (público, AUTH-03)
 * - GET lista: admin, atendente
 */
@Path("/os")
@RequestScoped
@Tag(name = "Ordens de Serviço", description = "Gerenciamento de OS")
public class OrdemDeServicoResource {

    @Inject
    OrdemDeServicoServicePort osService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Criar nova OS")
    @APIResponse(responseCode = "201", description = "OS criada com sucesso")
    public Response create(@Valid CreateOrdemDeServicoRequest request, @Context UriInfo uriInfo) {
        var command = new CreateOrdemDeServicoCommand(
                request.getClienteId(), request.getVeiculoId(), request.getDescricaoProblema());
        OrdemDeServico os = osService.create(command);
        OrdemDeServicoResponse response = toResponse(os);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Listar OS (paginado)")
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("createdAt,desc") String sort) {
        var content = osService.findAll(page, size, sort).stream().map(this::toResponse).toList();
        long total = osService.countAll();
        int totalPages = (int) Math.ceil((double) total / size);
        return Response.ok(new OrdemDeServicoPageResponse(content, page, size, total, totalPages)).build();
    }

    @GET
    @Path("/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Consultar status da OS (público)")
    @APIResponse(responseCode = "200", description = "Status da OS")
    public Response getStatus(@PathParam("id") UUID id) {
        OrdemDeServico os = osService.findById(id);
        var response = new OrdemDeServicoStatusResponse(os.getId(), os.getStatus().name(), os.getCreatedAt());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente", "mecanico"})
    @Operation(summary = "Buscar OS por ID")
    public Response getById(@PathParam("id") UUID id) {
        return Response.ok(toResponse(osService.findById(id))).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Atualizar dados da OS", description = "Atualiza cliente, veículo e descrição. Permitido apenas em RECEBIDA.")
    public Response update(@PathParam("id") UUID id, @Valid CreateOrdemDeServicoRequest request, @Context UriInfo uriInfo) {
        var command = new CreateOrdemDeServicoCommand(
                request.getClienteId(), request.getVeiculoId(), request.getDescricaoProblema());
        OrdemDeServico os = osService.update(id, command);
        return Response.ok(toResponse(os)).build();
    }

    @PUT
    @Path("/{id}/iniciar-diagnostico")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"mecanico", "admin"})
    @Operation(summary = "Iniciar diagnóstico da OS")
    public Response iniciarDiagnostico(@PathParam("id") UUID id) {
        return Response.ok(toResponse(osService.iniciarDiagnostico(id))).build();
    }

    @PUT
    @Path("/{id}/finalizar-diagnostico")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"mecanico", "admin"})
    @Operation(summary = "Finalizar diagnóstico")
    public Response finalizarDiagnostico(@PathParam("id") UUID id) {
        return Response.ok(toResponse(osService.finalizarDiagnostico(id))).build();
    }

    @PUT
    @Path("/{id}/aprovar-orcamento")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Aprovar orçamento → EM_EXECUCAO")
    public Response aprovarOrcamento(@PathParam("id") UUID id) {
        return Response.ok(toResponse(osService.aprovarOrcamento(id))).build();
    }

    @PUT
    @Path("/{id}/reprovar-orcamento")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Reprovar orçamento → CANCELADA")
    public Response reprovarOrcamento(@PathParam("id") UUID id, MotivoRequest body) {
        return Response.ok(toResponse(osService.reprovarOrcamento(id, body.motivo()))).build();
    }

    @PUT
    @Path("/{id}/cancelar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin"})
    @Operation(summary = "Cancelar OS")
    public Response cancelar(@PathParam("id") UUID id, MotivoRequest body) {
        return Response.ok(toResponse(osService.cancelar(id, body.motivo()))).build();
    }

    @PUT
    @Path("/{id}/finalizar")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"mecanico", "admin"})
    @Operation(summary = "Finalizar execução")
    public Response finalizar(@PathParam("id") UUID id) {
        return Response.ok(toResponse(osService.finalizar(id))).build();
    }

    @PUT
    @Path("/{id}/entregar")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Entregar veículo ao cliente")
    public Response entregar(@PathParam("id") UUID id) {
        return Response.ok(toResponse(osService.entregar(id))).build();
    }

    // ─────────────── Helper ───────────────

    private OrdemDeServicoResponse toResponse(OrdemDeServico os) {
        return new OrdemDeServicoResponse(
                os.getId(), os.getClienteId(), os.getVeiculoId(),
                os.getDescricaoProblema(), os.getStatus().name(),
                os.getMotivoCancelamento(), os.getCreatedAt()
        );
    }

    public record MotivoRequest(String motivo) {}
}
