package com.fiap.mekano.rest.api;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.application.service.MockPaymentService;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.rest.api.dto.CreateOrdemDeServicoRequest;
import com.fiap.mekano.rest.api.dto.FinalizarDiagnosticoRequest;
import com.fiap.mekano.rest.api.dto.FinalizarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.IniciarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoDetailResponse;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoPageResponse;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoResponse;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoStatusResponse;
import com.fiap.mekano.rest.api.dto.PagamentoResponse;
import com.fiap.mekano.rest.api.dto.RecebidoPorRequest;
import com.fiap.mekano.rest.api.dto.TempoMedioResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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

/**
 * Resource para Ordens de Serviço.
 *
 * Roles mistas (D-14, D-15):
 * - POST: admin, atendente
 * - PUT transições: mecanico, admin
     * - GET /status: @RolesAllowed (autenticado, AUTH-03)
 * - GET lista: admin, atendente
 */
@Path("/os")
@RequestScoped
@Tag(name = "Ordens de Serviço", description = "Gerenciamento de OS")
public class OrdemDeServicoResource {

    @Inject
    OrdemDeServicoServicePort osService;

    @Inject
    MockPaymentService paymentService;

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
    @RolesAllowed({ "admin", "atendente", "mecanico" })
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
    @RolesAllowed({"admin", "atendente", "mecanico", "cliente", "financeiro", "user"})
    @Operation(summary = "Consultar status da OS")
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"mecanico", "admin"})
    @Operation(summary = "Finalizar diagnóstico e gerar orçamento",
               description = "Finaliza o diagnóstico e dispara a geração automática do orçamento via evento. " +
                             "Os itens referenciam peças e serviços já cadastrados no sistema.")
    @APIResponse(responseCode = "200", description = "Diagnóstico finalizado, orçamento gerado automaticamente")
    public Response finalizarDiagnostico(@PathParam("id") UUID id, @Valid FinalizarDiagnosticoRequest request) {
        var itens = request.getItens().stream()
                .map(i -> new FinalizarDiagnosticoCommand.ItemDiagnostico(
                        i.getReferenciaUuid(), i.getTipo(), i.getQuantidade()))
                .toList();
        var command = new FinalizarDiagnosticoCommand(id, request.getDescricao(), itens);
        return Response.ok(toResponse(osService.finalizarDiagnostico(command))).build();
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

    @PATCH
    @Path("/{id}/entregar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Entregar veículo ao cliente")
    @APIResponse(responseCode = "200", description = "Entrega registrada")
    @APIResponse(responseCode = "422", description = "Pagamento pendente ou OS não finalizada")
    public Response entregar(@PathParam("id") UUID id, @Valid RecebidoPorRequest body) {
        return Response.ok(toResponse(osService.entregar(id, body.getRecebidoPor()))).build();
    }

    @PATCH
    @Path("/{id}/confirmar-pagamento")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "financeiro"})
    @Operation(summary = "Confirmar pagamento da OS")
    @APIResponse(responseCode = "200", description = "Pagamento confirmado com sucesso")
    @APIResponse(responseCode = "409", description = "Pagamento não está pendente")
    @APIResponse(responseCode = "503", description = "Mock de pagamento indisponível")
    public Response confirmarPagamento(@PathParam("id") UUID id) {
        paymentService.confirmarPagamento(id);
        var os = osService.findById(id);
        return Response.ok(new PagamentoResponse(
                id,
                os.getStatusPagamento().name(),
                os.getReferenciaPagamento(),
                os.getValorCobrado(),
                os.getPagamentoConfirmadoEm()
        )).build();
    }

    @PUT
    @Path("/{id}/iniciar-execucao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"mecanico", "admin"})
    @Operation(summary = "Iniciar execução da OS")
    @APIResponse(responseCode = "200", description = "Execução iniciada")
    @APIResponse(responseCode = "400", description = "OS não está em AGUARDANDO_APROVACAO")
    public Response iniciarExecucao(@PathParam("id") UUID id, @Valid IniciarExecucaoRequest request) {
        OrdemDeServico os = osService.iniciarExecucao(id, request.getMecanicoUuid(), request.getObservacao());
        return Response.ok(toResponse(os)).build();
    }

    @PUT
    @Path("/{id}/finalizar-execucao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"mecanico", "admin"})
    @Operation(summary = "Finalizar execução da OS")
    @APIResponse(responseCode = "200", description = "Execução finalizada, evento publicado")
    @APIResponse(responseCode = "400", description = "OS não está em EM_EXECUCAO")
    public Response finalizarExecucao(@PathParam("id") UUID id, @Valid FinalizarExecucaoRequest request) {
        OrdemDeServico os = osService.finalizarExecucao(id, request.getObservacao());
        return Response.ok(toResponse(os)).build();
    }

    @GET
    @Path("/{id}/detalhamento")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Detalhamento da OS com itens orçados")
    @APIResponse(responseCode = "200", description = "Detalhamento da OS")
    public Response getDetalhamento(@PathParam("id") UUID id) {
        OrdemDeServico os = osService.findById(id);
        var orcamentoUuid = osService.findOrcamentoUuidByOsId(id);
        var response = new OrdemDeServicoDetailResponse(
                os.getId(), os.getClienteId(), os.getVeiculoId(),
                os.getDescricaoProblema(), os.getStatus().name(),
                orcamentoUuid.orElse(null),
                os.getMecanicoUuid(),
                os.getExecucaoIniciadaEm(),
                os.getExecucaoFinalizadaEm(),
                os.getObservacaoExecucao(),
                os.getStatusPagamento() != null ? os.getStatusPagamento().name() : null,
                os.getStatusEntrega() != null ? os.getStatusEntrega().name() : null,
                os.getValorCobrado(),
                os.getReferenciaPagamento(),
                os.getRecebidoPor(),
                os.getPagamentoConfirmadoEm(),
                os.getEntregueEm(),
                Collections.singletonList("Itens orçados disponíveis no orçamento"),
                Collections.emptyList(),
                os.getCreatedAt()
        );
        return Response.ok(response).build();
    }

    @GET
    @Path("/tempo-medio")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Tempo médio de execução de OS")
    @APIResponse(responseCode = "200", description = "Tempo médio calculado")
    public Response getTempoMedio(
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim) {
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        var tempoMedio = osService.calcularTempoMedioExecucao(inicio, fim);
        return Response.ok(new TempoMedioResponse(tempoMedio.orElse(null))).build();
    }

    // ─────────────── Helper ───────────────

    private OrdemDeServicoResponse toResponse(OrdemDeServico os) {
        return new OrdemDeServicoResponse(
                os.getId(), os.getClienteId(), os.getVeiculoId(),
                os.getDescricaoProblema(), os.getStatus().name(),
                os.getMotivoCancelamento(), os.getOrcamentoUuid(),
                os.getMecanicoUuid(), os.getExecucaoIniciadaEm(),
                os.getExecucaoFinalizadaEm(), os.getObservacaoExecucao(),
                os.getStatusPagamento() != null ? os.getStatusPagamento().name() : null,
                os.getStatusEntrega() != null ? os.getStatusEntrega().name() : null,
                os.getValorCobrado(),
                os.getReferenciaPagamento(),
                os.getRecebidoPor(),
                os.getPagamentoConfirmadoEm(),
                os.getEntregueEm(),
                os.getCreatedAt()
        );
    }

    public record MotivoRequest(String motivo) {}
}
