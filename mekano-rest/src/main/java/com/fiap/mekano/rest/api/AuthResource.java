package com.fiap.mekano.rest.api;

import com.fiap.mekano.rest.api.dto.LoginRequest;
import com.fiap.mekano.rest.api.dto.LoginResponse;
import com.fiap.mekano.rest.api.exception.ErrorResponse;
import com.fiap.mekano.domain.exception.InvalidCredentialsException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.AuthenticateUserCommand;
import com.fiap.mekano.domain.port.in.AuthenticateUserInputPort;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.security.PrivateKey;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Duration;
import java.util.Set;

/**
 * Endpoint público de autenticação.
 *
 * <p>Fluxo:
 * <ol>
 *     <li>Recebe {@link LoginRequest} (email + password).</li>
 *     <li>Delega validação ao {@link AuthenticateUserInputPort} — busca user
 *         por email e compara senha com hash BCrypt.</li>
 *     <li>Emite JWT RS256 via {@link Jwt#issuer(String)} + assinatura com a
 *         chave privada apontada por {@code smallrye.jwt.sign.key.location}
 *         (ver {@code application.properties}).</li>
 *     <li>Devolve {@link LoginResponse} no shape OAuth 2.0 (RFC 6749 §5.1).</li>
 * </ol>
 *
 * <p>{@code @PermitAll}: este endpoint não exige autenticação prévia — é a
 * porta de entrada do fluxo. {@code @SecurityRequirement(name = "")} sobrescreve
 * o requirement global declarado em {@link MekanoApiApplication} para que o
 * Swagger UI <b>não</b> envie um header {@code Authorization} ao chamar este
 * endpoint mesmo após o usuário clicar em "Authorize".
 *
 * <p>Em caso de credenciais inválidas, o use case lança
 * {@link com.fiap.mekano.domain.exception.InvalidCredentialsException} →
 * {@link com.fiap.mekano.rest.api.exception.InvalidCredentialsExceptionMapper}
 * traduz para HTTP 401 + {@link ErrorResponse}.
 */
@Path("/auth")
@RequestScoped
@Tag(name = "Auth", description = "Authentication endpoints")
@SecurityRequirement(name = "")
public class AuthResource {

    @Inject
    AuthenticateUserInputPort authenticateUserInputPort;

    @Inject
    PrivateKey signingKey;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "mekano.auth.token.expiration-minutes", defaultValue = "60")
    long expirationMinutes;

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Login com email e senha",
            description = "Valida credenciais e emite um JWT RS256 que pode ser " +
                    "colado no botão 'Authorize' do Swagger UI para chamar endpoints protegidos.")
    @APIResponse(responseCode = "200",
            description = "Credenciais válidas — JWT emitido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LoginResponse.class)))
    @APIResponse(responseCode = "400",
            description = "Payload inválido (email/senha em branco ou formato incorreto)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "401",
            description = "Credenciais inválidas (email inexistente OU senha incorreta — mensagem única)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response login(
            @RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(name = "valido",
                                    value = "{\"email\":\"ana@fiap.br\",\"password\":\"abc123\"}")))
            @Valid LoginRequest request) throws InvalidCredentialsException {

        AuthenticateUserCommand command = new AuthenticateUserCommand(request.getEmail(), request.getPassword());
        User user = authenticateUserInputPort.execute(command);

        Duration ttl = Duration.ofMinutes(expirationMinutes);
        String token = Jwt.issuer(issuer)
                .subject(user.getId().toString())
                .upn(user.getEmail().getValue())
                .groups(Set.of("user"))
                .expiresIn(ttl)
                .sign(signingKey);

        LoginResponse response = new LoginResponse(token, "Bearer", ttl.toSeconds());
        return Response.ok(response).build();
    }
}
