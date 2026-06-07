package com.fiap.mekano.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO de resposta de login. Segue o shape padrão OAuth 2.0 / RFC 6749 §5.1
 * (campos em snake_case via {@link JsonProperty}) para compatibilidade com
 * clientes HTTP comuns e o botão "Authorize" do Swagger UI.
 *
 * @param accessToken JWT RS256 emitido pelo SmallRye JWT Build.
 * @param tokenType   Sempre {@code "Bearer"} (RFC 6750).
 * @param expiresIn   Tempo de vida do token em segundos.
 */
@Schema(description = "Resposta de login com JWT bearer token")
public record LoginResponse(
        @JsonProperty("access_token")
        @Schema(description = "JWT bearer token", example = "eyJhbGciOiJSUzI1NiIs...")
        String accessToken,

        @JsonProperty("token_type")
        @Schema(description = "Tipo do token (sempre Bearer)", example = "Bearer")
        String tokenType,

        @JsonProperty("expires_in")
        @Schema(description = "Tempo de vida do token em segundos", example = "3600")
        long expiresIn
) {}
