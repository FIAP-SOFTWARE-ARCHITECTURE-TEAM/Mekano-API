package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para criar uma requisição de compra")
public class CreateRequisicaoCompraRequest {

    @NotNull(message = "Peça é obrigatória")
    @Schema(required = true, description = "UUID da peça", examples = "550e8400-e29b-41d4-a716-446655440000")
    private UUID pecaId;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    @Schema(required = true, description = "Quantidade a comprar", examples = "10")
    private Integer quantidade;

    @NotBlank(message = "Motivo é obrigatório")
    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    @Schema(required = true, description = "Motivo da requisição", examples = "Reposição de estoque mínimo")
    private String motivo;
}
