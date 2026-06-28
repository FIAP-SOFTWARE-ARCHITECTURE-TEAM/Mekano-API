package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados da nota fiscal de entrada")
public record NfEntradaResponse(
                @Schema(description = "Identificador único da NF", examples = "550e8400-e29b-41d4-a716-446655440000") UUID id,
                @Schema(description = "Número da nota fiscal", examples = "123456") String numero,
                @Schema(description = "Série", examples = "1") String serie,
                @Schema(description = "CNPJ do fornecedor", examples = "12345678000190") String cnpjFornecedor,
                @Schema(description = "Nome do fornecedor", examples = "Auto Peças Ltda") String nomeFornecedor,
                @Schema(description = "Data de emissão (ISO-8601)", examples = "2026-06-01T10:00:00") LocalDateTime dataEmissao,
                @Schema(description = "Valor das mercadorias", examples = "1500.00") BigDecimal valorMercadoria,
                @Schema(description = "Valor ICMS", examples = "270.00") BigDecimal icms,
                @Schema(description = "Valor IPI", examples = "75.00") BigDecimal ipi,
                @Schema(description = "Valor outros impostos", examples = "30.00") BigDecimal outrosImpostos,
                @Schema(description = "Valor total calculado", examples = "1875.00") BigDecimal valorTotal,
                @Schema(description = "Chave de acesso NFe (44 dígitos)", examples = "35200612345678000190550000001234567890123456") String chaveAcesso,
                @Schema(description = "Data e hora de criação (ISO-8601)", examples = "2026-05-29T14:30:00") LocalDateTime createdAt) {
}
