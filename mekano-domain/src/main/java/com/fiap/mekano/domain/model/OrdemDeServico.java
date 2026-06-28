package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
 * </ul>
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class OrdemDeServico {

    private final UUID id;
    private final UUID clienteId;
    private final UUID veiculoId;
    private final String descricaoProblema;
    private StatusOS status;
    private String motivoCancelamento;
    private final LocalDateTime createdAt;
    private final Long version;

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
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp.
     */
    public static OrdemDeServico reconstitute(UUID id, UUID clienteId, UUID veiculoId,
                                              String descricaoProblema, StatusOS status,
                                              String motivoCancelamento,
                                              LocalDateTime createdAt, Long version) {
        return OrdemDeServico.builder()
                .id(id)
                .clienteId(clienteId)
                .veiculoId(veiculoId)
                .descricaoProblema(descricaoProblema)
                .status(status)
                .motivoCancelamento(motivoCancelamento)
                .createdAt(createdAt)
                .version(version)
                .build();
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
     * AGUARDANDO_APROVACAO → EM_EXECUCAO (aprovação direta, sem estado APROVADA — INC-01)
     */
    public void aprovarOrcamento() {
        transicionar(StatusOS.EM_EXECUCAO);
    }

    /**
     * AGUARDANDO_APROVACAO → CANCELADA (cliente reprovou o orçamento)
     */
    public void reprovarOrcamento(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new AppException(400, Messages.get("os.motivo_cancelamento.required"));
        }
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = motivo.strip();
    }

    /**
     * Qualquer estado não-terminal → CANCELADA (cancelamento genérico)
     */
    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new AppException(400, Messages.get("os.motivo_cancelamento.required"));
        }
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = motivo.strip();
    }

    /**
     * Qualquer estado não-terminal → CANCELADA (cancelamento por SLA)
     */
    public void cancelarPorSLA() {
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = "Cancelamento automático por SLA";
    }

    /**
     * EM_EXECUCAO → FINALIZADA
     */
    public void finalizar() {
        transicionar(StatusOS.FINALIZADA);
    }

    /**
     * FINALIZADA → ENTREGUE
     */
    public void entregar() {
        transicionar(StatusOS.ENTREGUE);
    }

    // ─────────────── Validação interna ───────────────

    private void transicionar(StatusOS destino) {
        if (!status.podeTransicionarPara(destino)) {
            throw new AppException(422, Messages.get("os.transicao.invalida", status, destino));
        }
        this.status = destino;
    }
}
