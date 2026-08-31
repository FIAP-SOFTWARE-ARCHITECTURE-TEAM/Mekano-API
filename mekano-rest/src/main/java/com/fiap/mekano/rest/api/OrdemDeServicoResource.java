package com.fiap.mekano.rest.api;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.application.service.MockPaymentService;
import com.fiap.mekano.domain.model.ItemOs;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.domain.port.in.CreateItemOsCommand;
import com.fiap.mekano.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.domain.port.out.ItemOsRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.rest.api.dto.CreateOrdemDeServicoRequest;
import com.fiap.mekano.rest.api.dto.FinalizarDiagnosticoRequest;
import com.fiap.mekano.rest.api.dto.FinalizarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.IniciarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoDetailResponse;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoPageResponse;
import com.fiap.mekano.rest.api.dto.ItemOsResponse;
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
 * - GET /status: @PermitAll (público, D-01/AUTH-03 — UUID é a chave de acesso,
 * D-02)
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

    @Inject
    ItemOsRepositoryPort itemOsRepository;

    @Inject
    OrcamentoRepositoryPort orcamentoRepository;

    @Inject
    PecaRepositoryPort pecaRepository;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Criar nova OS")
    @APIResponse(responseCode = "201", description = "OS criada com sucesso")
    public Response create(@Valid CreateOrdemDeServicoRequest request, @Context UriInfo uriInfo) {
        List<CreateItemOsCommand> itensCmd = request.getItens() != null
                ? request.getItens().stream()
                        .map(i -> new CreateItemOsCommand(i.getReferenciaUuid(), i.getTipo(),
                                i.getQuantidade() != null ? i.getQuantidade() : 1L))
                        .toList()
                : List.of();
        var command = new CreateOrdemDeServicoCommand(
                request.getClienteId(), request.getVeiculoId(),
                request.getDescricaoProblema(), itensCmd);
        OrdemDeServico os = osService.create(command);
        List<ItemOsResponse> itensResponse = fetchItensOs(os.getId());
        OrdemDeServicoResponse response = toResponse(os, itensResponse, false);
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
        var content = osService.findAll(page, size, sort).stream()
                .map(os -> toResponse(os, fetchItensOs(os.getId()),
                        calcularLiberadoParaExecucao(os)))
                .toList();
        long total = osService.countAll();
        int totalPages = (int) Math.ceil((double) total / size);
        return Response.ok(new OrdemDeServicoPageResponse(content, page, size, total, totalPages)).build();
    }

    @GET
    @Path("/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Consultar status da OS", description = "Consulta pública de status — UUID da OS é a chave de acesso (D-02)")
    @APIResponse(responseCode = "200", description = "Status da OS (consulta pública)")
    public Response getStatus(@PathParam("id") UUID id) {
        OrdemDeServico os = osService.findById(id);
        var response = new OrdemDeServicoStatusResponse(os.getId(), os.getStatus().name(), os.getCreatedAt());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente", "mecanico" })
    @Operation(summary = "Buscar OS por ID")
    public Response getById(@PathParam("id") UUID id) {
        OrdemDeServico os = osService.findById(id);
        List<ItemOsResponse> itensResponse = fetchItensOs(os.getId());
        return Response.ok(toResponse(os, itensResponse, false)).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Atualizar dados da OS", description = "Atualiza cliente, veículo e descrição. Permitido apenas em RECEBIDA.")
    public Response update(@PathParam("id") UUID id, @Valid CreateOrdemDeServicoRequest request,
            @Context UriInfo uriInfo) {
        List<CreateItemOsCommand> itensCmd = request.getItens() != null
                ? request.getItens().stream()
                        .map(i -> new CreateItemOsCommand(i.getReferenciaUuid(), i.getTipo(),
                                i.getQuantidade() != null ? i.getQuantidade() : 1L))
                        .toList()
                : List.of();
        var command = new CreateOrdemDeServicoCommand(
                request.getClienteId(), request.getVeiculoId(),
                request.getDescricaoProblema(), itensCmd);
        OrdemDeServico os = osService.update(id, command);
        List<ItemOsResponse> itensResponse = fetchItensOs(os.getId());
        return Response.ok(toResponse(os, itensResponse, false)).build();
    }

    @PUT
    @Path("/{id}/iniciar-diagnostico")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Iniciar diagnóstico da OS")
    public Response iniciarDiagnostico(@PathParam("id") UUID id) {
        OrdemDeServico os = osService.iniciarDiagnostico(id);
        return Response.ok(toResponse(os, fetchItensOs(os.getId()), false)).build();
    }

    @PUT
    @Path("/{id}/finalizar-diagnostico")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Finalizar diagnóstico e gerar orçamento", description = "Finaliza o diagnóstico e dispara a geração automática do orçamento via evento. "
            +
            "Os itens referenciam peças e serviços já cadastrados no sistema.")
    @APIResponse(responseCode = "200", description = "Diagnóstico finalizado, orçamento gerado automaticamente")
    public Response finalizarDiagnostico(@PathParam("id") UUID id, @Valid FinalizarDiagnosticoRequest request) {
        var itens = request.getItens().stream()
                .map(i -> new FinalizarDiagnosticoCommand.ItemDiagnostico(
                        i.getReferenciaUuid(), i.getTipo(), i.getQuantidade()))
                .toList();
        var command = new FinalizarDiagnosticoCommand(id, request.getDescricao(), itens);
        OrdemDeServico os = osService.finalizarDiagnostico(command);
        return Response.ok(toResponse(os, fetchItensOs(os.getId()), false)).build();
    }

    @PUT
    @Path("/{id}/cancelar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin" })
    @Operation(summary = "Cancelar OS")
    public Response cancelar(@PathParam("id") UUID id, MotivoRequest body) {
        OrdemDeServico os = osService.cancelar(id, body.motivo());
        return Response.ok(toResponse(os, fetchItensOs(os.getId()), false)).build();
    }

    @PATCH
    @Path("/{id}/entregar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Entregar veículo ao cliente")
    @APIResponse(responseCode = "200", description = "Entrega registrada")
    @APIResponse(responseCode = "422", description = "Pagamento pendente ou OS não finalizada")
    public Response entregar(@PathParam("id") UUID id, @Valid RecebidoPorRequest body) {
        OrdemDeServico os = osService.entregar(id, body.getRecebidoPor());
        return Response.ok(toResponse(os, fetchItensOs(os.getId()), false)).build();
    }

    @PATCH
    @Path("/{id}/confirmar-pagamento")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "financeiro" })
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
                os.getPagamentoConfirmadoEm())).build();
    }

    @PUT
    @Path("/{id}/iniciar-execucao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Iniciar execução da OS")
    @APIResponse(responseCode = "200", description = "Execução iniciada")
    @APIResponse(responseCode = "400", description = "OS não está em AGUARDANDO_APROVACAO")
    public Response iniciarExecucao(@PathParam("id") UUID id, @Valid IniciarExecucaoRequest request) {
        OrdemDeServico os = osService.iniciarExecucao(id, request.getMecanicoUuid(), request.getObservacao());
        return Response.ok(toResponse(os, fetchItensOs(os.getId()), false)).build();
    }

    @PUT
    @Path("/{id}/finalizar-execucao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "mecanico", "admin" })
    @Operation(summary = "Finalizar execução da OS")
    @APIResponse(responseCode = "200", description = "Execução finalizada, evento publicado")
    @APIResponse(responseCode = "400", description = "OS não está em EM_EXECUCAO")
    public Response finalizarExecucao(@PathParam("id") UUID id, @Valid FinalizarExecucaoRequest request) {
        OrdemDeServico os = osService.finalizarExecucao(id, request.getObservacao());
        return Response.ok(toResponse(os, fetchItensOs(os.getId()), false)).build();
    }

    @GET
    @Path("/{id}/detalhamento")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Detalhamento da OS com itens orçados")
    @APIResponse(responseCode = "200", description = "Detalhamento da OS")
    public Response getDetalhamento(@PathParam("id") UUID id) {
        OrdemDeServico os = osService.findById(id);
        var orcamentoUuid = osService.findOrcamentoUuidByOsId(id);
        var itensOrcados = osService.buscarItensOrcados(id);
        List<ItemOsResponse> itensResponse = fetchItensOs(os.getId());
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
                itensResponse,
                itensOrcados,
                Collections.emptyList(),
                os.getCreatedAt());
        return Response.ok(response).build();
    }

    @GET
    @Path("/tempo-medio")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Tempo médio de execução de OS com breakdown por mecânico")
    @APIResponse(responseCode = "200", description = "Tempo médio calculado")
    public Response getTempoMedio(
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim) {
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        var tempoMedio = osService.calcularTempoMedioExecucao(inicio, fim);
        var breakdown = osService.calcularTempoMedioPorMecanico(inicio, fim);
        return Response.ok(new TempoMedioResponse(tempoMedio.orElse(null), breakdown)).build();
    }

    @GET
    @Path("/filtro")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ "admin", "atendente" })
    @Operation(summary = "Listar OS com filtros (status, cliente, veículo, data)")
    @APIResponse(responseCode = "200", description = "Lista filtrada de OS")
    public Response findAllWithFilters(
            @QueryParam("status") String status,
            @QueryParam("clienteId") UUID clienteId,
            @QueryParam("veiculoId") UUID veiculoId,
            @QueryParam("dataInicio") LocalDate dataInicio,
            @QueryParam("dataFim") LocalDate dataFim,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;
        var content = osService.findAllWithFilters(status, clienteId, veiculoId, inicio, fim, page, size)
                .stream().map(os -> toResponse(os, fetchItensOs(os.getId()),
                        calcularLiberadoParaExecucao(os)))
                .toList();
        return Response.ok(new OrdemDeServicoPageResponse(content, page, size, content.size(), 1)).build();
    }

    // ─────────────── Helper ───────────────

    private List<ItemOsResponse> fetchItensOs(UUID osUuid) {
        return itemOsRepository.findByOsUuid(osUuid).stream()
                .map(item -> new ItemOsResponse(
                        item.getId(), item.getReferenciaUuid(), item.getTipo(),
                        item.getDescricao(), item.getQuantidade()))
                .toList();
    }

    private OrdemDeServicoResponse toResponse(OrdemDeServico os, List<ItemOsResponse> itens,
            boolean liberadoParaExecucao) {
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
                os.getCreatedAt(),
                itens,
                liberadoParaExecucao);
    }

    private boolean calcularLiberadoParaExecucao(OrdemDeServico os) {
        if (os.getOrcamentoUuid() == null) {
            return false;
        }
        var orcamento = orcamentoRepository.findByUuid(os.getOrcamentoUuid());
        if (orcamento.isEmpty() || orcamento.get().getStatus() != StatusOrcamento.APROVADO) {
            return false;
        }
        List<ItemOs> itens = itemOsRepository.findByOsUuid(os.getId());
        List<ItemOs> pecas = itens.stream().filter(ItemOs::isPeca).toList();
        for (ItemOs peca : pecas) {
            var pecaOpt = pecaRepository.findById(peca.getReferenciaUuid());
            if (pecaOpt.isEmpty()) {
                return false;
            }
            Long disponivel = pecaOpt.get().disponivel();
            if (disponivel < peca.getQuantidade()) {
                return false;
            }
        }
        return true;
    }

    public record MotivoRequest(String motivo) {
    }
}
