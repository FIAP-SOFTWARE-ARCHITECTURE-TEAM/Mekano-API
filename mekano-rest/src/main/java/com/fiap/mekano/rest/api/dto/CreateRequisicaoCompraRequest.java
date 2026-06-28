package com.fiap.mekano.rest.api.dto;

import com.fiap.mekano.domain.model.MotivoRequisicao;
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
@Schema(description = "Request payload para criar uma requisição de compra")
public class CreateRequisicaoCompraRequest {

    @NotNull(message = "Peça é obrigatória")
    @Schema(required = true, description = "UUID da peça", examples = "550e8400-e29b-41d4-a716-446655440000")
    private UUID pecaId;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    @Schema(required = true, description = "Quantidade a comprar", examples = "10")
    private Integer quantidade;

    @NotNull(message = "Motivo é obrigatório")
    @Schema(required = true, description = "Motivo da requisição", examples = "ESTOQUE_MINIMO")
    private MotivoRequisicao motivo;
}
