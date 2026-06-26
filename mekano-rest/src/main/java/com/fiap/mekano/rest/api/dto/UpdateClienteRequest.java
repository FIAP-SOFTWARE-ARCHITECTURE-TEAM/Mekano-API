package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para atualizacao de um cliente (sem CPF)")
public class UpdateClienteRequest {

    @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
    @Schema(description = "Nome do cliente", example = "Joao Silva")
    private String nome;

    @Email(message = "Email deve ter formato valido")
    @Size(max = 255, message = "Email deve ter no maximo 255 caracteres")
    @Schema(description = "Email do cliente", example = "joao@email.com")
    private String email;

    @Size(max = 11, message = "Telefone deve ter no maximo 11 digitos")
    @Schema(description = "Telefone do cliente", example = "11999887766")
    private String telefone;

    @Size(max = 255)
    @Schema(description = "Logradouro", example = "Rua das Flores")
    private String logradouro;

    @Size(max = 20)
    @Schema(description = "Numero", example = "123")
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

    @Size(min = 8, max = 8)
    @Schema(description = "CEP (8 digitos)", example = "01001000")
    private String cep;
}
