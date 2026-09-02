package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Item de uma requisição de compra")
public class ItemRequisicaoCompraRequest {

    @NotNull(message = "UUID da peça é obrigatório")
    @Schema(required = true, description = "UUID da peça/insumo", examples = "550e8400-e29b-41d4-a716-446655440000")
    private UUID pecaUuid;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    @Schema(required = true, description = "Quantidade solicitada", examples = "10")
    private Integer quantidade;
}
