package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request para atualização de veículo")
public class UpdateVeiculoRequest {

    @NotBlank
    @Pattern(regexp = "^(?:[A-Z]{3}[0-9]{4}|[A-Z]{3}[0-9][A-Z][0-9]{2}|[A-Z]{3}-[0-9]{4})$", message = "Placa inválida")
    @Schema(description = "Placa do veículo no padrão Mercosul ou antigo", examples = "ABC1D23")
    private String placa;

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
