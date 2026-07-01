package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecebidoPorRequest {

    @NotBlank(message = "Nome de quem recebeu o veículo é obrigatório")
    private String recebidoPor;
}
