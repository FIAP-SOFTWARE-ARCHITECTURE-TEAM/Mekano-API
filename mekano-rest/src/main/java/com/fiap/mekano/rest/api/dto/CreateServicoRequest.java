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

/**
 * DTO de entrada para criação de serviço.
 *
 * <p>Classe (não record) para compatibilidade com MapStruct 1.6.x.
 * Validação {@code @DecimalMin} garante valor > 0 na camada adapter.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para criação de um novo serviço")
public class CreateServicoRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    @Schema(required = true, description = "Nome do serviço", example = "Troca de óleo")
    private String nome;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    @Schema(description = "Descrição do serviço", example = "Troca de óleo do motor com filtro incluso")
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    @Schema(required = true, description = "Valor do serviço em reais", example = "89.90")
    private BigDecimal valor;
}
