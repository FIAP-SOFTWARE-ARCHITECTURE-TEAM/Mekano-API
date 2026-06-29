package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReprovarMotivoRequest(
        @NotBlank(message = "Motivo da reprovação é obrigatório") String motivo
) {}