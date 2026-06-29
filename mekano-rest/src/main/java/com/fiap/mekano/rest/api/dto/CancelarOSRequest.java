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
public class CancelarOSRequest {

    @NotNull(message = "UUID da OS é obrigatório")
    private UUID osUuid;

    @NotBlank(message = "Motivo do cancelamento é obrigatório")
    private String motivo;
}