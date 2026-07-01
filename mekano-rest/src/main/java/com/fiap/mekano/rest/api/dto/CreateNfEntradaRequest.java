package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para registrar uma nota fiscal de entrada")
public class CreateNfEntradaRequest {

    @NotBlank(message = "Chave de acesso é obrigatória")
    @Size(min = 44, max = 44, message = "Chave de acesso deve ter exatamente 44 dígitos")
    @Schema(required = true, description = "Chave de acesso NFe (44 dígitos)", examples = "35200612345678000190550000001234567890123456")
    private String chaveAcesso;

    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor total deve ser maior que zero")
    @Schema(required = true, description = "Valor total da NF em reais", examples = "1875.00")
    private BigDecimal valorTotal;

    @NotNull(message = "Requisição de compra é obrigatória")
    @Schema(required = true, description = "UUID da requisição de compra vinculada", examples = "550e8400-e29b-41d4-a716-446655440000")
    private UUID requisicaoCompraId;
}
