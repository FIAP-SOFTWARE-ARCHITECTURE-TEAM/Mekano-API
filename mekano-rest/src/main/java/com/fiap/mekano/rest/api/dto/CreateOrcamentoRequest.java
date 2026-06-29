package com.fiap.mekano.rest.api.dto;

import com.fiap.mekano.domain.model.ItemOrcamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrcamentoRequest {

    @NotNull(message = "UUID da OS é obrigatório")
    private UUID ordemServicoUuid;

    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;

    @NotEmpty(message = "Ao menos um item é obrigatório")
    @Valid
    private List<ItemOrcamentoRequest> itens;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemOrcamentoRequest {
        @NotBlank(message = "Descrição do item é obrigatória")
        private String descricao;

        @NotNull(message = "Quantidade é obrigatória")
        private Long quantidade;

        @NotNull(message = "Valor unitário é obrigatório")
        private java.math.BigDecimal valorUnitario;
    }
}