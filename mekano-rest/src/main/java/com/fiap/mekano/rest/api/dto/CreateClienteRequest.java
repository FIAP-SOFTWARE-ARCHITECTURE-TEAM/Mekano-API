package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para criacao de um novo cliente")
public class CreateClienteRequest {

    @NotBlank(message = "Nome e obrigatorio")
    @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
    @Schema(required = true, description = "Nome do cliente", example = "Joao Silva")
    private String nome;

    @NotBlank(message = "CPF e obrigatorio")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter exatamente 11 digitos")
    @Schema(required = true, description = "CPF do cliente (11 digitos)", example = "52998224725")
    private String cpf;

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ter formato valido")
    @Size(max = 255, message = "Email deve ter no maximo 255 caracteres")
    @Schema(required = true, description = "Email do cliente", example = "joao@email.com")
    private String email;

    @Size(max = 11, message = "Telefone deve ter no maximo 11 digitos")
    @Schema(description = "Telefone do cliente (DDD + numero)", example = "11999887766")
    private String telefone;

    @Size(max = 255)
    @Schema(description = "Logradouro do endereco", example = "Rua das Flores")
    private String logradouro;

    @Size(max = 20)
    @Schema(description = "Numero do endereco", example = "123")
    private String numero;

    @Size(max = 255)
    @Schema(description = "Bairro", example = "Centro")
    private String bairro;

    @Size(max = 255)
    @Schema(description = "Cidade", example = "Sao Paulo")
    private String cidade;

    @Size(min = 2, max = 2)
    @Schema(description = "UF (2 caracteres)", example = "SP")
    private String uf;

    @Pattern(regexp = "\\d{8}", message = "CEP deve conter exatamente 8 digitos")
    @Schema(description = "CEP (8 digitos)", example = "01001000")
    private String cep;
}
