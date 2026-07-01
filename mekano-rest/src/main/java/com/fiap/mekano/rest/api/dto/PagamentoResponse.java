package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resposta da confirmação de pagamento")
public record PagamentoResponse(
        @Schema(description = "UUID da OS") UUID osUuid,
        @Schema(description = "Status do pagamento") String status,
        @Schema(description = "ID da transação") String transacaoId,
        @Schema(description = "Valor cobrado") BigDecimal valorCobrado,
        @Schema(description = "Data do pagamento") LocalDateTime dataPagamento
) {}
