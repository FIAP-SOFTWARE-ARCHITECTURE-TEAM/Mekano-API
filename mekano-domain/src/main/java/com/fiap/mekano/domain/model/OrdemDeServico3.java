/*
 * package com.fiap.mekano.domain.model;
 * 
 * import com.fiap.mekano.domain.exception.AppException; import
 * com.fiap.mekano.domain.exception.Messages; import
 * com.fiap.mekano.domain.os.StatusEntrega; import
 * com.fiap.mekano.domain.os.StatusPagamento;
 * 
 * import lombok.AccessLevel; import lombok.Builder; import lombok.Getter;
 * import lombok.ToString;
 * 
 * import java.time.LocalDateTime; import java.util.UUID;
 * 
 *//**
	 * Entidade de domínio OrdemDeServico — aggregate root do bounded context de
	 * oficina.
	 *
	 * <p>
	 * Máquina de estados com 7 status (sem APROVADA — INC-01). Transições via
	 * métodos explícitos, NUNCA setStatus() (D-26). {@code @Version} para
	 * optimistic locking será adicionado na entity JPA (infrastructure).
	 *
	 * <p>
	 * Regras:
	 * <ul>
	 * <li>Criação via {@link #create} — status inicial RECEBIDA</li>
	 * <li>Reconstrução via {@link #reconstitute} — preserva valores do banco</li>
	 * <li>Cada transição valida contra a matriz do enum StatusOS</li>
	 * <li>ENTREGUE e CANCELADA são terminais — nenhuma transição de saída</li>
	 * </ul>
	 */
/*
 * @Getter
 * 
 * @Builder(access = AccessLevel.PRIVATE)
 * 
 * @ToString public class OrdemDeServico3 {
 * 
 * private final UUID id; private UUID clienteId; private UUID veiculoId;
 * private String descricaoProblema; private StatusOS status; private String
 * motivoCancelamento; private UUID orcamentoUuid; private UUID mecanicoUuid;
 * private LocalDateTime execucaoIniciadaEm; private LocalDateTime
 * execucaoFinalizadaEm; private String observacaoExecucao; private
 * LocalDateTime dataAprovacao; private final LocalDateTime createdAt; private
 * final Long version; private StatusPagamento statusPagamento; private
 * StatusEntrega statusEntrega; private LocalDateTime cobrancaGeradaEm; private
 * LocalDateTime pagamentoConfirmadoEm; private String referenciaPagamento;
 * private LocalDateTime entregueEm; private String recebidoPor;
 * 
 * 
 *//**
	 * Factory method — cria uma nova OS com status RECEBIDA.
	 *
	 * @param clienteId         UUID do cliente
	 * @param veiculoId         UUID do veículo
	 * @param descricaoProblema descrição do problema relatado pelo cliente
	 * @return nova OrdemDeServico com status RECEBIDA
	 */
/*
 * public static OrdemDeServico create(UUID clienteId, UUID veiculoId, String
 * descricaoProblema) { if (clienteId == null) { throw new AppException(400,
 * Messages.get("os.cliente.required")); } if (veiculoId == null) { throw new
 * AppException(400, Messages.get("os.veiculo.required")); } if
 * (descricaoProblema == null || descricaoProblema.isBlank()) { throw new
 * AppException(400, Messages.get("os.descricao.required")); }
 * 
 * return OrdemDeServico3.builder() .id(UUID.randomUUID()) .clienteId(clienteId)
 * .veiculoId(veiculoId) .descricaoProblema(descricaoProblema.strip())
 * .status(StatusOS.RECEBIDA) .statusPagamento(StatusPagamento.NAO_COBRADO)
 * .statusEntrega(StatusEntrega.NAO_LIBERADA) .createdAt(LocalDateTime.now())
 * .version(0L) .build(); }
 * 
 *//**
	 * Factory method para reconstrução a partir de dados persistidos. NÃO gera novo
	 * UUID nem timestamp.
	 */
/*
 * 
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
 * .createdAt(createdAt) .version(version).build(); }
 * 
 * 
 *//**
	 * Factory method completo para reconstrução incluindo cobrança, pagamento e
	 * entrega.
	 */
/*
 * public static OrdemDeServico reconstitute(UUID id, UUID clienteId, UUID
 * veiculoId, String descricaoProblema, StatusOS status, String
 * motivoCancelamento, UUID orcamentoUuid, UUID mecanicoUuid, LocalDateTime
 * execucaoIniciadaEm, LocalDateTime execucaoFinalizadaEm, String
 * observacaoExecucao, LocalDateTime dataAprovacao, LocalDateTime createdAt,
 * Long version, StatusPagamento statusPagamento, StatusEntrega statusEntrega,
 * LocalDateTime cobrancaGeradaEm, LocalDateTime pagamentoConfirmadoEm, String
 * referenciaPagamento, LocalDateTime entregueEm, String recebidoPor) { return
 * OrdemDeServico.builder() .id(id) .clienteId(clienteId) .veiculoId(veiculoId)
 * .descricaoProblema(descricaoProblema) .status(status)
 * .motivoCancelamento(motivoCancelamento) .orcamentoUuid(orcamentoUuid)
 * .mecanicoUuid(mecanicoUuid) .execucaoIniciadaEm(execucaoIniciadaEm)
 * .execucaoFinalizadaEm(execucaoFinalizadaEm)
 * .observacaoExecucao(observacaoExecucao) .dataAprovacao(dataAprovacao)
 * .createdAt(createdAt) .version(version) .statusPagamento(statusPagamento ==
 * null ? StatusPagamento.NAO_COBRADO : statusPagamento)
 * .statusEntrega(statusEntrega == null ? StatusEntrega.NAO_LIBERADA :
 * statusEntrega) .cobrancaGeradaEm(cobrancaGeradaEm)
 * .pagamentoConfirmadoEm(pagamentoConfirmadoEm)
 * .referenciaPagamento(referenciaPagamento) .entregueEm(entregueEm)
 * .recebidoPor(recebidoPor) .build(); }
 * 
 * 
 * // ─────────────── Atualização de dados (somente RECEBIDA) ───────────────
 * 
 *//**
	 * Atualiza dados cadastrais da OS. Permitido APENAS em RECEBIDA.
	 */
/*
 * public void atualizar(UUID clienteId, UUID veiculoId, String
 * descricaoProblema) { if (status != StatusOS.RECEBIDA) { throw new
 * AppException(422, Messages.get("os.transicao.invalida", status,
 * "ATUALIZAR")); } if (clienteId == null) { throw new AppException(400,
 * Messages.get("os.cliente.required")); } if (veiculoId == null) { throw new
 * AppException(400, Messages.get("os.veiculo.required")); } if
 * (descricaoProblema == null || descricaoProblema.isBlank()) { throw new
 * AppException(400, Messages.get("os.descricao.required")); } this.clienteId =
 * clienteId; this.veiculoId = veiculoId; this.descricaoProblema =
 * descricaoProblema.strip(); }
 * 
 * 
 * 
 * // ─────────────── Transições explícitas (D-26) ───────────────
 * 
 *//**
	 * RECEBIDA → EM_DIAGNOSTICO
	 */
/*
 * public void iniciarDiagnostico() { transicionar(StatusOS.EM_DIAGNOSTICO); }
 * 
 *//**
	 * EM_DIAGNOSTICO → AGUARDANDO_APROVACAO
	 */
/*
 * public void finalizarDiagnostico() {
 * transicionar(StatusOS.AGUARDANDO_APROVACAO); }
 * 
 *//**
	 * Associa um orçamento à OS sem transicionar status. Chamado por
	 * OrcamentoService.gerarOrcamento() ao gerar o orçamento.
	 */
/*
 * public void associarOrcamento(UUID orcamentoUuid) { if (orcamentoUuid ==
 * null) { throw new AppException(400,
 * Messages.get("os.orcamento_uuid.required")); } this.orcamentoUuid =
 * orcamentoUuid; }
 * 
 *//**
	 * AGUARDANDO_APROVACAO → EM_EXECUCAO (aprovação direta, sem estado APROVADA —
	 * INC-01) Armazena orcamentoUuid e dataAprovacao.
	 */
/*
 * public void aprovarOrcamento(UUID orcamentoUuid) {
 * transicionar(StatusOS.EM_EXECUCAO); if (orcamentoUuid != null) {
 * this.orcamentoUuid = orcamentoUuid; } this.dataAprovacao =
 * LocalDateTime.now(); }
 * 
 *//**
	 * AGUARDANDO_APROVACAO → CANCELADA (cliente reprovou o orçamento)
	 */
/*
 * public void reprovarOrcamento(String motivo) { if (motivo == null ||
 * motivo.isBlank()) { throw new AppException(400,
 * Messages.get("os.motivo_cancelamento.required")); }
 * transicionar(StatusOS.CANCELADA); this.motivoCancelamento = motivo.strip(); }
 * 
 *//**
	 * Qualquer estado não-terminal → CANCELADA (cancelamento genérico)
	 */
/*
 * public void cancelar(String motivo) { if (motivo == null || motivo.isBlank())
 * { throw new AppException(400,
 * Messages.get("os.motivo_cancelamento.required")); }
 * transicionar(StatusOS.CANCELADA); this.motivoCancelamento = motivo.strip(); }
 * 
 *//**
	 * Qualquer estado não-terminal → CANCELADA (cancelamento por SLA)
	 */
/*
 * public void cancelarPorSLA() { transicionar(StatusOS.CANCELADA);
 * this.motivoCancelamento = "Cancelamento automático por SLA"; }
 * 
 *//**
	 * EM_EXECUCAO → FINALIZADA
	 */
/*
 * public void finalizar() { transicionar(StatusOS.FINALIZADA); }
 * 
 *//**
	 * EM_EXECUCAO → FINALIZADA com observação de execução
	 */
/*
 * public void finalizarExecucao(String observacao) {
 * transicionar(StatusOS.FINALIZADA); this.execucaoFinalizadaEm =
 * LocalDateTime.now(); if (observacao != null && !observacao.isBlank()) {
 * this.observacaoExecucao = observacao.strip(); } }
 * 
 *//**
	 * AGUARDANDO_APROVACAO → EM_EXECUCAO + registra mecânico e início (execução
	 * direta pós-aprovação)
	 */
/*
 * public void iniciarExecucao(UUID mecanicoUuid, String observacao) { if
 * (mecanicoUuid == null) { throw new AppException(400,
 * Messages.get("os.mecanico.required")); } this.mecanicoUuid = mecanicoUuid;
 * this.execucaoIniciadaEm = LocalDateTime.now(); if (observacao != null &&
 * !observacao.isBlank()) { this.observacaoExecucao = observacao.strip(); } }
 * 
 *//**
	 * FINALIZADA → ENTREGUE
	 *//*
		 * public void entregar() { transicionar(StatusOS.ENTREGUE); }
		 * 
		 * // ─────────────── Validação interna ───────────────
		 * 
		 * private void transicionar(StatusOS destino) { if
		 * (!status.podeTransicionarPara(destino)) { throw new AppException(422,
		 * Messages.get("os.transicao.invalida", status, destino)); } this.status =
		 * destino; } }
		 */