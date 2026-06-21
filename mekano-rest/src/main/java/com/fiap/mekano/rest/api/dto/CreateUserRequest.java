package com.fiap.mekano.rest.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO de entrada para criação de usuário.
 *
 * Implementado como classe (não record) para compatibilidade com MapStruct 1.6.x:
 * MapStruct requer getters para ler campos da source.
 * Lombok @Getter gera os getters necessários.
 *
 * A validação de formato de email (@Email) é feita aqui na camada adapter.
 * A senha é enviada em plaintext — o hash BCrypt é responsabilidade de CreateUserUseCase.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para criação de um novo usuário")
public class CreateUserRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Schema(required = true, description = "Nome do usuário", example = "Ana Lima")
    private String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    @Schema(required = true, description = "Email do usuário — deve ser único no sistema", example = "ana@fiap.br")
    private String email;

    @NotNull(message = "Senha é obrigatória")
    @Size(min = 6, max = 128, message = "Senha deve ter entre 6 e 128 caracteres")
    @Schema(required = true, minLength = 6, maxLength = 128, description = "Senha do usuário em plaintext — será armazenada como hash BCrypt", example = "abc123")
    private String password;
}
