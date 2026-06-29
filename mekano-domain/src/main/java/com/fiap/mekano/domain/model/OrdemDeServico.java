package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.event.CobrancaGeradaEvent;
import com.fiap.mekano.domain.event.EntregaConfirmadaEvent;
import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.os.StatusEntrega;
import com.fiap.mekano.domain.os.StatusPagamento;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de domínio OrdemDeServico — aggregate root do bounded context de oficina.
 *
 * <p>Máquina de estados com 7 status (sem APROVADA — INC-01).
 * Transições via métodos explícitos, NUNCA setStatus() (D-26).
 * {@code @Version} para optimistic locking será adicionado na entity JPA (infrastructure).
 *
 * <p>Regras:
 * <ul>
 *   <li>Criação via {@link #create} — status inicial RECEBIDA</li>
 *   <li>Reconstrução via {@link #reconstitute} — preserva valores do banco</li>
 *   <li>Cada transição valida contra a matriz do enum StatusOS</li>
 *   <li>ENTREGUE e CANCELADA são terminais — nenhuma transição de saída</li>
 *   <li>Cobrança só pode ser gerada após OS FINALIZADA</li>
 *   <li>Pagamento só pode ser confirmado após cobrança gerada</li>
 *   <li>Entrega só pode ocorrer após pagamento confirmado</li>
 * </ul>
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class OrdemDeServico {

    private final UUID id;
    private UUID clienteId;
    private UUID veiculoId;
    private String descricaoProblema;
    private StatusOS status;
    private String motivoCancelamento;
    private UUID orcamentoUuid;
    private UUID mecanicoUuid;
    private LocalDateTime execucaoIniciadaEm;
    private LocalDateTime execucaoFinalizadaEm;
    private String observacaoExecucao;
    private LocalDateTime dataAprovacao;
    // TODO(#33): campos de pagamento — dependem da migration V18
    //            (ALTER TABLE ordens_de_servico ADD COLUMN status_pagamento, etc.)
    //            e dos campos na OrdemDeServicoEntity
    private StatusPagamento statusPagamento;
    private BigDecimal valorCobrado;
    private LocalDateTime dataPagamento;
    private LocalDateTime dataEntrega;
    private String observacaoEntrega;
    private final LocalDateTime createdAt;
    private final Long version;

    private StatusPagamento statusPagamento;
    private StatusEntrega statusEntrega;
    private LocalDateTime cobrancaGeradaEm;
    private LocalDateTime pagamentoConfirmadoEm;
    private String referenciaPagamento;
    private LocalDateTime entregueEm;
    private String recebidoPor;

    /**
     * Factory method — cria uma nova OS com status RECEBIDA.
     *
     * @param clienteId UUID do cliente
     * @param veiculoId UUID do veículo
     * @param descricaoProblema descrição do problema relatado pelo cliente
     * @return nova OrdemDeServico com status RECEBIDA
     */
    public static OrdemDeServico create(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        if (clienteId == null) {
            throw new AppException(400, Messages.get("os.cliente.required"));
        }
        if (veiculoId == null) {
            throw new AppException(400, Messages.get("os.veiculo.required"));
        }
        if (descricaoProblema == null || descricaoProblema.isBlank()) {
            throw new AppException(400, Messages.get("os.descricao.required"));
        }

        return OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .clienteId(clienteId)
                .veiculoId(veiculoId)
                .descricaoProblema(descricaoProblema.strip())
                .status(StatusOS.RECEBIDA)
                .createdAt(LocalDateTime.now())
                .version(0L)
                .statusPagamento(StatusPagamento.NAO_COBRADO)
                .statusEntrega(StatusEntrega.NAO_LIBERADA)
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp.
     *
     * Mantém a assinatura antiga para não quebrar adapters/repositórios existentes.
     */
	/*
	 * public static OrdemDeServico reconstitute(UUID id, UUID clienteId, UUID
	 * veiculoId, String descricaoProblema, StatusOS status, String
	 * motivoCancelamento, UUID orcamentoUuid, UUID mecanicoUuid, LocalDateTime
	 * execucaoIniciadaEm, LocalDateTime execucaoFinalizadaEm, String
	 * observacaoExecucao, LocalDateTime dataAprovacao, LocalDateTime createdAt,
	 * Long version) {
	 * 
	 * return OrdemDeServico.builder() .id(id) .clienteId(clienteId)
	 * .veiculoId(veiculoId) .descricaoProblema(descricaoProblema) .status(status)
	 * .motivoCancelamento(motivoCancelamento) .orcamentoUuid(orcamentoUuid)
	 * .mecanicoUuid(mecanicoUuid) .execucaoIniciadaEm(execucaoIniciadaEm)
	 * .execucaoFinalizadaEm(execucaoFinalizadaEm)
	 * .observacaoExecucao(observacaoExecucao) .dataAprovacao(dataAprovacao)
	 * .createdAt(createdAt) .version(version)
	 * .statusPagamento(StatusPagamento.NAO_COBRADO)
	 * .statusEntrega(StatusEntrega.NAO_LIBERADA) .build(); }
	 */

    /**
     * Factory method completo para reconstrução incluindo cobrança, pagamento e entrega.
     */
    public static OrdemDeServico reconstitute(UUID id, UUID clienteId, UUID veiculoId,
                                              String descricaoProblema, StatusOS status,
                                              String motivoCancelamento,
                                              UUID orcamentoUuid, UUID mecanicoUuid,
                                              LocalDateTime execucaoIniciadaEm,
                                              LocalDateTime execucaoFinalizadaEm,
                                              String observacaoExecucao,
                                              LocalDateTime dataAprovacao,
                                              StatusPagamento statusPagamento,
                                              BigDecimal valorCobrado,
                                              LocalDateTime dataPagamento,
                                              LocalDateTime dataEntrega,
                                              String observacaoEntrega,
                                              LocalDateTime createdAt, Long version) {
        return OrdemDeServico.builder()
                .id(id)
                .clienteId(clienteId)
                .veiculoId(veiculoId)
                .descricaoProblema(descricaoProblema)
                .status(status)
                .motivoCancelamento(motivoCancelamento)
                .orcamentoUuid(orcamentoUuid)
                .mecanicoUuid(mecanicoUuid)
                .execucaoIniciadaEm(execucaoIniciadaEm)
                .execucaoFinalizadaEm(execucaoFinalizadaEm)
                .observacaoExecucao(observacaoExecucao)
                .dataAprovacao(dataAprovacao)
                .statusPagamento(statusPagamento)
                .valorCobrado(valorCobrado)
                .dataPagamento(dataPagamento)
                .dataEntrega(dataEntrega)
                .observacaoEntrega(observacaoEntrega)
                .createdAt(createdAt)
                .version(version)
                .statusPagamento(statusPagamento == null ? StatusPagamento.NAO_COBRADO : statusPagamento)
                .statusEntrega(statusEntrega == null ? StatusEntrega.NAO_LIBERADA : statusEntrega)
                .cobrancaGeradaEm(cobrancaGeradaEm)
                .pagamentoConfirmadoEm(pagamentoConfirmadoEm)
                .referenciaPagamento(referenciaPagamento)
                .entregueEm(entregueEm)
                .recebidoPor(recebidoPor)
                .build();
    }

    // ─────────────── Atualização de dados (somente RECEBIDA) ───────────────

    /**
     * Atualiza dados cadastrais da OS. Permitido APENAS em RECEBIDA.
     */
    public void atualizar(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        if (status != StatusOS.RECEBIDA) {
            throw new AppException(422, Messages.get("os.transicao.invalida", status, "ATUALIZAR"));
        }
        if (clienteId == null) {
            throw new AppException(400, Messages.get("os.cliente.required"));
        }
        if (veiculoId == null) {
            throw new AppException(400, Messages.get("os.veiculo.required"));
        }
        if (descricaoProblema == null || descricaoProblema.isBlank()) {
            throw new AppException(400, Messages.get("os.descricao.required"));
        }

        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.descricaoProblema = descricaoProblema.strip();
    }

    // ─────────────── Transições explícitas (D-26) ───────────────

    /**
     * RECEBIDA → EM_DIAGNOSTICO
     */
    public void iniciarDiagnostico() {
        transicionar(StatusOS.EM_DIAGNOSTICO);
    }

    /**
     * EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
     */
    public void finalizarDiagnostico() {
        transicionar(StatusOS.AGUARDANDO_APROVACAO);
    }

    /**
     * Associa um orçamento à OS sem transicionar status.
     * Chamado por OrcamentoService.gerarOrcamento() ao gerar o orçamento.
     */
    public void associarOrcamento(UUID orcamentoUuid) {
        if (orcamentoUuid == null) {
            throw new AppException(400, Messages.get("os.orcamento_uuid.required"));
        }

        this.orcamentoUuid = orcamentoUuid;
    }

    /**
     * AGUARDANDO_APROVACAO → EM_EXECUCAO.
     * Armazena orcamentoUuid e dataAprovacao.
     */
    public void aprovarOrcamento(UUID orcamentoUuid) {
        transicionar(StatusOS.EM_EXECUCAO);

        if (orcamentoUuid != null) {
            this.orcamentoUuid = orcamentoUuid;
        }

        this.dataAprovacao = LocalDateTime.now();
    }

    /**
     * AGUARDANDO_APROVACAO → CANCELADA.
     */
    public void reprovarOrcamento(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new AppException(400, Messages.get("os.motivo_cancelamento.required"));
        }

        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = motivo.strip();
        cancelarPagamentoEEntrega();
    }

    /**
     * Qualquer estado não-terminal → CANCELADA.
     */
    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new AppException(400, Messages.get("os.motivo_cancelamento.required"));
        }

        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = motivo.strip();
        cancelarPagamentoEEntrega();
    }

    /**
     * Qualquer estado não-terminal → CANCELADA.
     */
    public void cancelarPorSLA() {
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = "Cancelamento automático por SLA";
        cancelarPagamentoEEntrega();
    }

    /**
     * EM_EXECUCAO → FINALIZADA.
     */
    public void finalizar() {
        transicionar(StatusOS.FINALIZADA);
    }

    /**
     * EM_EXECUCAO → FINALIZADA com observação de execução.
     */
    public void finalizarExecucao(String observacao) {
        transicionar(StatusOS.FINALIZADA);
        this.execucaoFinalizadaEm = LocalDateTime.now();

        if (observacao != null && !observacao.isBlank()) {
            this.observacaoExecucao = observacao.strip();
        }
    }

    /**
     * Registra mecânico e início da execução.
     *
     * <p>Não altera status porque a transição para EM_EXECUCAO já ocorre em aprovarOrcamento().
     */
    public void iniciarExecucao(UUID mecanicoUuid, String observacao) {
        if (mecanicoUuid == null) {
            throw new AppException(400, Messages.get("os.mecanico.required"));
        }

        if (status != StatusOS.EM_EXECUCAO) {
            throw new AppException(422, Messages.get("os.transicao.invalida", status, "INICIAR_EXECUCAO"));
        }

        this.mecanicoUuid = mecanicoUuid;
        this.execucaoIniciadaEm = LocalDateTime.now();

        if (observacao != null && !observacao.isBlank()) {
            this.observacaoExecucao = observacao.strip();
        }
    }

    // ─────────────── Cobrança, pagamento e entrega ───────────────

    /**
     * FINALIZADA + NAO_COBRADO → AGUARDANDO_PAGAMENTO.
     */
    public CobrancaGeradaEvent gerarCobranca() {
        if (status != StatusOS.FINALIZADA) {
            throw new AppException(422, Messages.get("os.cobranca.status.invalido", status));
        }

        if (!statusPagamento.podeTransicionarPara(StatusPagamento.AGUARDANDO_PAGAMENTO)) {
            throw new AppException(422, Messages.get("os.cobranca.ja.gerada", statusPagamento));
        }

        this.statusPagamento = StatusPagamento.AGUARDANDO_PAGAMENTO;
        this.cobrancaGeradaEm = LocalDateTime.now();

        return CobrancaGeradaEvent.of(this.id);
    }

    /**
     * Variante útil quando a cobrança deve ficar vinculada a um orçamento específico.
     */
    public CobrancaGeradaEvent gerarCobranca(UUID orcamentoUuid) {
        if (orcamentoUuid == null) {
            throw new AppException(400, Messages.get("os.orcamento_uuid.required"));
        }

        this.orcamentoUuid = orcamentoUuid;
        return gerarCobranca();
    }

    /**
     * AGUARDANDO_PAGAMENTO → CONFIRMADO.
     * Ao confirmar pagamento, a entrega é liberada.
     */
    public PagamentoConfirmadoEvent confirmarPagamento(String referenciaPagamento) {
        if (referenciaPagamento == null || referenciaPagamento.isBlank()) {
            throw new AppException(400, Messages.get("os.pagamento.referencia.required"));
        }

        if (!statusPagamento.podeTransicionarPara(StatusPagamento.CONFIRMADO)) {
            throw new AppException(422, Messages.get("os.pagamento.status.invalido", statusPagamento));
        }

        this.statusPagamento = StatusPagamento.CONFIRMADO;
        this.pagamentoConfirmadoEm = LocalDateTime.now();
        this.referenciaPagamento = referenciaPagamento.strip();

        liberarEntrega();

        return PagamentoConfirmadoEvent.of(this.id, this.referenciaPagamento);
    }

    /**
     * FINALIZADA + pagamento CONFIRMADO + entrega LIBERADA → ENTREGUE.
     */
    public EntregaConfirmadaEvent entregar(String recebidoPor) {
        if (recebidoPor == null || recebidoPor.isBlank()) {
            throw new AppException(400, Messages.get("os.entrega.recebedor.required"));
        }

        if (status != StatusOS.FINALIZADA) {
            throw new AppException(422, Messages.get("os.transicao.invalida", status, StatusOS.ENTREGUE));
        }

        if (statusPagamento != StatusPagamento.CONFIRMADO) {
            throw new AppException(422, Messages.get("os.entrega.pagamento.pendente", statusPagamento));
        }

        if (!statusEntrega.podeTransicionarPara(StatusEntrega.ENTREGUE)) {
            throw new AppException(422, Messages.get("os.entrega.status.invalido", statusEntrega));
        }

        transicionar(StatusOS.ENTREGUE);
        this.dataEntrega = LocalDateTime.now();
    }

    /**
     * Entrega com observação (service-level guard valida pagamento antes)
     */
    public void entregar(String observacao) {
        transicionar(StatusOS.ENTREGUE);
        this.dataEntrega = LocalDateTime.now();
        if (observacao != null && !observacao.isBlank()) {
            this.observacaoEntrega = observacao.strip();
        }
    }

    // ─────────────── Pagamento ───────────────

    public boolean isPagamentoPendente() {
        return statusPagamento == StatusPagamento.PENDENTE;
    }

    public boolean isPagamentoConfirmado() {
        return statusPagamento == StatusPagamento.CONFIRMADO;
    }

    /**
     * Emite cobrança: seta PENDENTE com valor copiado do orçamento (D-03)
     */
    public void emitirCobranca(BigDecimal valor) {
        if (statusPagamento != null) {
            throw new AppException(409, "Cobrança já existe para esta OS");
        }
        this.statusPagamento = StatusPagamento.PENDENTE;
        this.valorCobrado = valor;
        this.dataPagamento = LocalDateTime.now();
    }

    /**
     * Confirma pagamento: PENDENTE → CONFIRMADO (D-02)
     */
    public void confirmarPagamento() {
        if (statusPagamento != StatusPagamento.PENDENTE) {
            throw new AppException(409, "Pagamento não está pendente");
        }
        this.statusPagamento = StatusPagamento.CONFIRMADO;
        this.dataPagamento = LocalDateTime.now();
    }

    // ─────────────── Validação interna ───────────────

    private void transicionar(StatusOS destino) {
        if (!status.podeTransicionarPara(destino)) {
            throw new AppException(422, Messages.get("os.transicao.invalida", status, destino));
        }

        this.status = destino;
    }

    private void liberarEntrega() {
        if (statusEntrega.podeTransicionarPara(StatusEntrega.LIBERADA_PARA_ENTREGA)) {
            this.statusEntrega = StatusEntrega.LIBERADA_PARA_ENTREGA;
        }
    }

    private void cancelarPagamentoEEntrega() {
        if (statusPagamento != StatusPagamento.CONFIRMADO
                && statusPagamento.podeTransicionarPara(StatusPagamento.CANCELADO)) {
            this.statusPagamento = StatusPagamento.CANCELADO;
        }

        this.statusEntrega = StatusEntrega.NAO_LIBERADA;
    }
}