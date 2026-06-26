package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados do cliente")
public record ClienteResponse(
        @Schema(description = "Identificador unico do cliente") UUID id,
        @Schema(description = "Nome do cliente") String nome,
        @Schema(description = "CPF do cliente (11 digitos)") String cpf,
        @Schema(description = "Email do cliente") String email,
        @Schema(description = "Telefone do cliente") String telefone,
        @Schema(description = "Logradouro") String enderecoLogradouro,
        @Schema(description = "Numero") String enderecoNumero,
        @Schema(description = "Bairro") String enderecoBairro,
        @Schema(description = "Cidade") String enderecoCidade,
        @Schema(description = "UF") String enderecoUf,
        @Schema(description = "CEP") String enderecoCep,
        @Schema(description = "Data de criacao") LocalDateTime createdAt
) {}
