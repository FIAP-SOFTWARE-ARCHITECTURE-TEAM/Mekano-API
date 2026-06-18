package com.fiap.mekano.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO de entrada para login (POST /auth/login).
 *
 * Validação @Email/@NotBlank é aplicada na camada adapter; falhas viram 400
 * via {@code ConstraintViolationExceptionMapper}. A senha trafega em plaintext
 * sobre HTTPS — a comparação ao hash BCrypt é responsabilidade do
 * {@code AuthenticateUserUseCase}.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para login de usuário")
public class LoginRequest {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Schema(required = true, description = "Email cadastrado", example = "ana@fiap.br")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(required = true, description = "Senha em plaintext (validada contra hash BCrypt)", example = "abc123")
    private String password;
}
