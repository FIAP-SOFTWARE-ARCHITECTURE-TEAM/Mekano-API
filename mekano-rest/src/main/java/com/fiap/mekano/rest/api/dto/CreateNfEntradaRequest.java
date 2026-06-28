package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para registrar uma nota fiscal de entrada")
public class CreateNfEntradaRequest {

    @NotBlank(message = "Número da NF é obrigatório")
    @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
    @Schema(required = true, description = "Número da nota fiscal", examples = "123456")
    private String numero;

    @NotBlank(message = "Série é obrigatória")
    @Size(max = 5, message = "Série deve ter no máximo 5 caracteres")
    @Schema(required = true, description = "Série da nota fiscal", examples = "1")
    private String serie;

    @NotBlank(message = "CNPJ do fornecedor é obrigatório")
    @Size(min = 14, max = 14, message = "CNPJ deve ter exatamente 14 dígitos")
    @Schema(required = true, description = "CNPJ do fornecedor (apenas dígitos)", examples = "12345678000190")
    private String cnpjFornecedor;

    @NotBlank(message = "Nome do fornecedor é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    @Schema(required = true, description = "Nome do fornecedor", examples = "Auto Peças Ltda")
    private String nomeFornecedor;

    @NotNull(message = "Data de emissão é obrigatória")
    @Schema(required = true, description = "Data de emissão (ISO-8601)", examples = "2026-06-01T10:00:00")
    private LocalDateTime dataEmissao;

    @NotNull(message = "Valor da mercadoria é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor da mercadoria deve ser maior que zero")
    @Schema(required = true, description = "Valor das mercadorias em reais", examples = "1500.00")
    private BigDecimal valorMercadoria;

    @NotNull(message = "Valor ICMS é obrigatório")
    @DecimalMin(value = "0.00", message = "ICMS não pode ser negativo")
    @Schema(required = true, description = "Valor do ICMS", examples = "270.00")
    private BigDecimal icms;

    @NotNull(message = "Valor IPI é obrigatório")
    @DecimalMin(value = "0.00", message = "IPI não pode ser negativo")
    @Schema(required = true, description = "Valor do IPI", examples = "75.00")
    private BigDecimal ipi;

    @NotNull(message = "Valor outros impostos é obrigatório")
    @DecimalMin(value = "0.00", message = "Outros impostos não podem ser negativos")
    @Schema(required = true, description = "Valor de outros impostos", examples = "30.00")
    private BigDecimal outrosImpostos;

    @NotBlank(message = "Chave de acesso é obrigatória")
    @Size(min = 44, max = 44, message = "Chave de acesso deve ter exatamente 44 dígitos")
    @Schema(required = true, description = "Chave de acesso NFe (44 dígitos)", examples = "35200612345678000190550000001234567890123456")
    private String chaveAcesso;

    @NotNull(message = "Peça é obrigatória")
    @Schema(required = true, description = "UUID da peça a creditar", examples = "550e8400-e29b-41d4-a716-446655440000")
    private UUID pecaId;

    @NotNull(message = "Requisição de compra é obrigatória")
    @Schema(required = true, description = "UUID da requisição de compra vinculada", examples = "550e8400-e29b-41d4-a716-446655440000")
    private UUID requisicaoCompraId;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    @Schema(required = true, description = "Quantidade recebida", examples = "10")
    private Integer quantidade;
}
