package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request para item de Ordem de Serviço (peça ou serviço)")
public class CreateItemOsRequest {

    @NotNull(message = "Referência é obrigatória")
    @Schema(required = true, description = "UUID da peça ou serviço")
    private UUID referenciaUuid;

    @NotBlank(message = "Tipo é obrigatório")
    @Schema(required = true, description = "Tipo do item: PECA ou SERVICO")
    private String tipo;

    @Schema(description = "Quantidade (padrão: 1 para serviços)")
    private Long quantidade;
}
