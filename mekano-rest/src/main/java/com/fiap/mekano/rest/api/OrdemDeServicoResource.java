package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.port.in.CancelarOSCommand;
import com.fiap.mekano.domain.port.in.FinalizarExecucaoCommand;
import com.fiap.mekano.domain.port.in.IniciarExecucaoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.rest.api.dto.CancelarOSRequest;
import com.fiap.mekano.rest.api.dto.CreateOrdemDeServicoRequest;
import com.fiap.mekano.rest.api.dto.FinalizarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.IniciarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoPageResponse;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoResponse;
import com.fiap.mekano.rest.api.dto.TempoMedioResponse;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
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

@Path("/ordens-de-servico")
@RequestScoped
@RolesAllowed({"admin", "atendente", "mecanico"})
@Tag(name = "Ordens de Serviço", description = "Gerenciamento do ciclo de vida de ordens de serviço")
public class OrdemDeServicoResource {

    @Inject
    OrdemDeServicoServicePort ordemDeServicoService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Criar ordem de serviço", description = "Registra uma nova OS com status RECEBIDA")
    @APIResponse(responseCode = "201", description = "OS criada com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    public Response criar(@Valid CreateOrdemDeServicoRequest request, @Context UriInfo uriInfo) {
        var command = new com.fiap.mekano.domain.port.in.CriarOSCommand(
                request.getClienteId(), request.getVeiculoId(), request.getDescricaoProblema());
        var os = ordemDeServicoService.criar(command);
        var response = OrdemDeServicoResponse.from(os);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar OS por UUID", description = "Retorna detalhes completos da OS")
    @APIResponse(responseCode = "200", description = "OS encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    @APIResponse(responseCode = "404", description = "OS não encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response buscarPorId(@PathParam("uuid") UUID uuid) {
        var os = ordemDeServicoService.buscarPorId(uuid);
        return Response.ok(OrdemDeServicoResponse.from(os)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar OS", description = "Lista ordens de serviço com paginação e filtros")
    @APIResponse(responseCode = "200", description = "Lista paginada de OS",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoPageResponse.class)))
    public Response listar(
            @QueryParam("status") String status,
            @QueryParam("clienteUuid") UUID clienteUuid,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        var content = ordemDeServicoService.listarComFiltros(status, clienteUuid, page, size)
                .stream().map(OrdemDeServicoResponse::from).toList();
        long total = ordemDeServicoService.contar();
        int totalPages = (int) Math.ceil((double) total / Math.max(size, 1));
        return Response.ok(new OrdemDeServicoPageResponse(content, page, size, total, totalPages)).build();
    }

    @POST
    @Path("/{uuid}/iniciar-diagnostico")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Iniciar diagnóstico", description = "Transiciona OS de RECEBIDA para EM_DIAGNOSTICO")
    @APIResponse(responseCode = "200", description = "Diagnóstico iniciado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    @APIResponse(responseCode = "422", description = "Transição inválida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response iniciarDiagnostico(@PathParam("uuid") UUID uuid) {
        var os = ordemDeServicoService.iniciarDiagnostico(uuid);
        return Response.ok(OrdemDeServicoResponse.from(os)).build();
    }

    @POST
    @Path("/{uuid}/finalizar-diagnostico")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Finalizar diagnóstico", description = "Transiciona OS de EM_DIAGNOSTICO para AGUARDANDO_APROVACAO")
    @APIResponse(responseCode = "200", description = "Diagnóstico finalizado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    @APIResponse(responseCode = "422", description = "Transição inválida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response finalizarDiagnostico(@PathParam("uuid") UUID uuid) {
        var os = ordemDeServicoService.finalizarDiagnostico(uuid);
        return Response.ok(OrdemDeServicoResponse.from(os)).build();
    }

    @PUT
    @Path("/{uuid}/iniciar-execucao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "mecanico"})
    @Operation(summary = "Iniciar execução", description = "Registra início da execução da OS pelo mecânico")
    @APIResponse(responseCode = "200", description = "Execução iniciada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    @APIResponse(responseCode = "422", description = "OS não está EM_EXECUCAO",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response iniciarExecucao(@PathParam("uuid") UUID uuid, @Valid IniciarExecucaoRequest request) {
        var os = ordemDeServicoService.iniciarExecucao(
                new IniciarExecucaoCommand(uuid, request.getMecanicoUuid(), request.getObservacao()));
        return Response.ok(OrdemDeServicoResponse.from(os)).build();
    }

    @PUT
    @Path("/{uuid}/finalizar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "mecanico"})
    @Operation(summary = "Finalizar OS", description = "Finaliza a execução da OS com observação opcional")
    @APIResponse(responseCode = "200", description = "OS finalizada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    @APIResponse(responseCode = "422", description = "Transição inválida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response finalizar(@PathParam("uuid") UUID uuid, FinalizarExecucaoRequest request) {
        var os = ordemDeServicoService.finalizarExecucao(
                new FinalizarExecucaoCommand(uuid, request.getObservacao()));
        return Response.ok(OrdemDeServicoResponse.from(os)).build();
    }

    @POST
    @Path("/{uuid}/entregar")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Entregar OS", description = "Transiciona OS de FINALIZADA para ENTREGUE")
    @APIResponse(responseCode = "200", description = "OS entregue ao cliente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    @APIResponse(responseCode = "422", description = "Transição inválida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response entregar(@PathParam("uuid") UUID uuid) {
        var os = ordemDeServicoService.entregar(uuid);
        return Response.ok(OrdemDeServicoResponse.from(os)).build();
    }

    @POST
    @Path("/{uuid}/cancelar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cancelar OS", description = "Cancela a OS com motivo obrigatório")
    @APIResponse(responseCode = "200", description = "OS cancelada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrdemDeServicoResponse.class)))
    @APIResponse(responseCode = "422", description = "Transição inválida",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response cancelar(@PathParam("uuid") UUID uuid, @Valid CancelarOSRequest request) {
        var os = ordemDeServicoService.cancelar(new CancelarOSCommand(uuid, request.getMotivo()));
        return Response.ok(OrdemDeServicoResponse.from(os)).build();
    }

    @GET
    @Path("/metricas/tempo-medio")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Tempo médio de execução", description = "Retorna a média em horas das OS finalizadas")
    @APIResponse(responseCode = "200", description = "Média calculada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TempoMedioResponse.class)))
    public Response tempoMedioExecucao() {
        var media = ordemDeServicoService.calcularTempoMedioExecucao();
        return Response.ok(new TempoMedioResponse(media.orElse(0.0))).build();
    }

    @DELETE
    @Path("/{uuid}")
    @Operation(summary = "Excluir OS", description = "Soft delete da OS")
    @APIResponse(responseCode = "204", description = "OS excluída")
    @APIResponse(responseCode = "404", description = "OS não encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response deletar(@PathParam("uuid") UUID uuid) {
        ordemDeServicoService.deletar(uuid);
        return Response.noContent().build();
    }
}