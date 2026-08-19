package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request para atualização de veículo (sem placa — imutável após criação)")
public class UpdateVeiculoRequest {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "Marca do veículo", examples = "Volkswagen")
    private String marca;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "Modelo do veículo", examples = "Gol 1.6 MSI")
    private String modelo;

    @NotNull
    @Schema(description = "Ano de fabricação do veículo", examples = "2020")
    private Integer ano;
}