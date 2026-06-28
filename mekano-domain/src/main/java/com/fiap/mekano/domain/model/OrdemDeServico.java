package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public static OrdemDeServico create(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        if (clienteId == null) throw new AppException(400, Messages.get("os.cliente.required"));
        if (veiculoId == null) throw new AppException(400, Messages.get("os.veiculo.required"));
        if (descricaoProblema == null || descricaoProblema.isBlank())
            throw new AppException(400, Messages.get("os.descricao.required"));

        return OrdemDeServico.builder()
                .id(UUID.randomUUID()).clienteId(clienteId).veiculoId(veiculoId)
                .descricaoProblema(descricaoProblema.strip())
                .status(StatusOS.RECEBIDA).createdAt(LocalDateTime.now()).version(0L).build();
    }

    public static OrdemDeServico reconstitute(UUID id, UUID clienteId, UUID veiculoId,
                                              String descricaoProblema, StatusOS status,
                                              String motivoCancelamento, LocalDateTime createdAt, Long version) {
        return OrdemDeServico.builder()
                .id(id).clienteId(clienteId).veiculoId(veiculoId)
                .descricaoProblema(descricaoProblema).status(status)
                .motivoCancelamento(motivoCancelamento).createdAt(createdAt).version(version).build();
    }

    public void iniciarDiagnostico() { transicionar(StatusOS.EM_DIAGNOSTICO); }
    public void finalizarDiagnostico() { transicionar(StatusOS.AGUARDANDO_APROVACAO); }
    public void aprovarOrcamento() { transicionar(StatusOS.EM_EXECUCAO); }

    public void reprovarOrcamento(String motivo) {
        if (motivo == null || motivo.isBlank()) throw new AppException(400, Messages.get("os.motivo_cancelamento.required"));
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = motivo.strip();
    }

    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) throw new AppException(400, Messages.get("os.motivo_cancelamento.required"));
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = motivo.strip();
    }

    public void cancelarPorSLA() {
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = "Cancelamento automático por SLA";
    }

    public void finalizar() { transicionar(StatusOS.FINALIZADA); }
    public void entregar() { transicionar(StatusOS.ENTREGUE); }

    private void transicionar(StatusOS destino) {
        if (!status.podeTransicionarPara(destino))
            throw new AppException(422, Messages.get("os.transicao.invalida", status, destino));
        this.status = destino;
    }
}
