package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request para criação de uma Ordem de Serviço")
public class CreateOrdemDeServicoRequest {

    @NotNull(message = "Cliente é obrigatório")
    @Schema(required = true, description = "UUID do cliente")
    private UUID clienteId;

    @NotNull(message = "Veículo é obrigatório")
    @Schema(required = true, description = "UUID do veículo")
    private UUID veiculoId;

    @NotBlank(message = "Descrição do problema é obrigatória")
    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    @Schema(required = true, description = "Descrição do problema relatado")
    private String descricaoProblema;
}
