package com.fiap.mekano.rest.api.dto;

import java.util.UUID;

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
@Schema(description = "Request para criação de veículo")
public class CreateVeiculoRequest {

    @NotNull
    private UUID clienteUuid;

    @NotBlank
    @Pattern(regexp = "^(?:[A-Z]{3}[0-9]{4}|[A-Z]{3}[0-9][A-Z][0-9]{2}|[A-Z]{3}-[0-9]{4})$", message = "Placa inválida")
    private String placa;

    @NotBlank
    @Size(max = 50)
    private String marca;

    @NotBlank
    @Size(max = 50)
    private String modelo;

    @NotNull
    private Integer ano;
}