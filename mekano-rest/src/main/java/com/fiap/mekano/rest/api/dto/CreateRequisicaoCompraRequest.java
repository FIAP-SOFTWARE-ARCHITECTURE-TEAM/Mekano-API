package com.fiap.mekano.rest.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fiap.mekano.domain.model.MotivoRequisicao;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para criar uma requisição de compra com múltiplos itens")
public class CreateRequisicaoCompraRequest {

    @NotNull(message = "Motivo é obrigatório")
    @Schema(required = true, description = "Motivo da requisição", examples = "ESTOQUE_MINIMO")
    private MotivoRequisicao motivo;

    @NotEmpty(message = "Requisição deve conter ao menos um item")
    @Valid
    @Schema(required = true, description = "Lista de itens (peças/insumos) a comprar")
    private List<ItemRequisicaoCompraRequest> itens;
}
