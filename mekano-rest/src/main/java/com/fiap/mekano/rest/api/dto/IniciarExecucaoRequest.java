package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class IniciarExecucaoRequest {

    @NotNull(message = "UUID do mecânico é obrigatório")
    private UUID mecanicoUuid;

    private String observacao;
}