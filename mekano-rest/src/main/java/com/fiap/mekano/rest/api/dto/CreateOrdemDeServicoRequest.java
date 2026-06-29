package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrdemDeServicoRequest {

    @NotNull(message = "Cliente é obrigatório")
    private UUID clienteId;

    @NotNull(message = "Veículo é obrigatório")
    private UUID veiculoId;

    @NotBlank(message = "Descrição do problema é obrigatória")
    private String descricaoProblema;
}