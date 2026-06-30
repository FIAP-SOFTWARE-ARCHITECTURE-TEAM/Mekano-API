package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detalhamento da Ordem de Serviço com itens orçados e executados")
public record OrdemDeServicoDetailResponse(
        @Schema(description = "UUID da OS") UUID id,
        @Schema(description = "UUID do cliente") UUID clienteId,
        @Schema(description = "UUID do veículo") UUID veiculoId,
        @Schema(description = "Descrição do problema") String descricaoProblema,
        @Schema(description = "Status atual") String status,
        @Schema(description = "UUID do orçamento") UUID orcamentoUuid,
        @Schema(description = "UUID do mecânico responsável") UUID mecanicoUuid,
        @Schema(description = "Início da execução") LocalDateTime execucaoIniciadaEm,
        @Schema(description = "Fim da execução") LocalDateTime execucaoFinalizadaEm,
        @Schema(description = "Observação da execução") String observacaoExecucao,
        @Schema(description = "Status do pagamento") String statusPagamento,
        @Schema(description = "Status da entrega") String statusEntrega,
        @Schema(description = "Valor cobrado") BigDecimal valorCobrado,
        @Schema(description = "Referência do pagamento") String referenciaPagamento,
        @Schema(description = "Quem recebeu o veículo") String recebidoPor,
        @Schema(description = "Data do pagamento") LocalDateTime pagamentoConfirmadoEm,
        @Schema(description = "Data da entrega") LocalDateTime entregueEm,
        @Schema(description = "Itens orçados") List<String> itensOrcados,
        @Schema(description = "Itens executados") List<String> itensExecutados,
        @Schema(description = "Data de criação") LocalDateTime createdAt
) {}
