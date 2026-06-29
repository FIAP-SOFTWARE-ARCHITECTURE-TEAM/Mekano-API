package com.fiap.mekano.rest.api.dto;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@RegisterForReflection
@Schema(description = "Dados da Ordem de Serviço")
public record OrdemDeServicoResponse(
        @Schema(description = "UUID da OS") UUID id,
        @Schema(description = "UUID do cliente") UUID clienteId,
        @Schema(description = "UUID do veículo") UUID veiculoId,
        @Schema(description = "Descrição do problema") String descricaoProblema,
        @Schema(description = "Status atual") StatusOS status,
        @Schema(description = "Motivo de cancelamento (se cancelada)") String motivoCancelamento,
        @Schema(description = "UUID do orçamento aprovado") UUID orcamentoUuid,
        @Schema(description = "UUID do mecânico responsável") UUID mecanicoUuid,
        @Schema(description = "Início da execução") LocalDateTime execucaoIniciadaEm,
        @Schema(description = "Fim da execução") LocalDateTime execucaoFinalizadaEm,
        @Schema(description = "Observação da execução") String observacaoExecucao,
        @Schema(description = "Data de aprovação do orçamento") LocalDateTime dataAprovacao,
        @Schema(description = "Data de criação") LocalDateTime createdAt
) {
    public static OrdemDeServicoResponse from(OrdemDeServico os) {
        return new OrdemDeServicoResponse(
                os.getId(), os.getClienteId(), os.getVeiculoId(),
                os.getDescricaoProblema(), os.getStatus(), os.getMotivoCancelamento(),
                os.getOrcamentoUuid(), os.getMecanicoUuid(),
                os.getExecucaoIniciadaEm(), os.getExecucaoFinalizadaEm(),
                os.getObservacaoExecucao(), os.getDataAprovacao(),
                os.getCreatedAt());
    }
}