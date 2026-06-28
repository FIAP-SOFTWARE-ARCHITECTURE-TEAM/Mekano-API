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

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para cadastro de uma nova peça")
public class CreatePecaRequest {

    @NotBlank(message = "Código é obrigatório")
    @Size(max = 20, message = "Código deve ter no máximo 20 caracteres")
    @Schema(required = true, description = "Código identificador da peça", examples = "PEA-001")
    private String codigo;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    @Schema(required = true, description = "Descrição da peça", examples = "Óleo do Motor 5W30")
    private String descricao;

    @NotBlank(message = "Unidade de medida é obrigatória")
    @Schema(required = true, description = "Unidade de medida", examples = "LITRO")
    private String unidadeMedida;

    @NotNull(message = "Valor unitário é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor unitário não pode ser negativo")
    @Schema(required = true, description = "Valor unitário em reais", examples = "45.90")
    private BigDecimal valorUnitario;

    @Schema(description = "Estoque mínimo para gerar alerta", examples = "10")
    private Long estoqueMinimo;
}
