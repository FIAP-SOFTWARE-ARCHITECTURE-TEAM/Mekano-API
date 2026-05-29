package com.fiap.mekano.adapter.in.rest;

import com.fiap.mekano.adapter.in.rest.dto.CreateUserRequest;
import com.fiap.mekano.adapter.in.rest.dto.UserResponse;
import com.fiap.mekano.adapter.in.rest.exception.ErrorResponse;
import com.fiap.mekano.adapter.in.rest.mapper.UserDtoMapper;
import com.fiap.mekano.domain.port.in.CreateUserInputPort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;

/**
 * JAX-RS Resource para operações de usuário.
 *
 * Responsabilidades:
 * - Receber requisições HTTP POST /users
 * - Delegar validação ao Bean Validation (@Valid)
 * - Converter DTO → Command → User → DTO via UserDtoMapper
 * - Invocar CreateUserInputPort (nunca CreateUserUseCase diretamente — INH-04)
 * - Construir resposta 201 Created com Location header (D-01)
 *
 * Exceções de domínio são propagadas — ExceptionMappers registrados (Plans 04)
 * interceptam e convertem para 409/404/400.
 *
 * @RequestScoped: ciclo de vida por requisição — necessário para @Context UriInfo funcionar
 *                 corretamente como parâmetro de método em RESTEasy Reactive.
 * @Transactional: PROIBIDO neste resource — transações são responsabilidade de infrastructure (INH-06).
 */
@Path("/users")
@RequestScoped
@Tag(name = "Users", description = "User management")
public class UserResource {

    @Inject
    CreateUserInputPort createUserInputPort;

    @Inject
    UserDtoMapper userDtoMapper;

    /**
     * Cria um novo usuário no sistema.
     *
     * Fluxo:
     * 1. Bean Validation valida CreateUserRequest (@Valid)
     * 2. UserDtoMapper converte request → CreateUserCommand
     * 3. CreateUserInputPort.execute() orquestra: verifica duplicidade → hash BCrypt → User.create() → save()
     * 4. UserDtoMapper converte User → UserResponse (Email VO → String)
     * 5. UriInfo constrói Location absoluta: http://host/users/{uuid}
     * 6. Response.created(uri).entity(response) retorna 201
     *
     * Exceções:
     * - ConstraintViolationException → ConstraintViolationExceptionMapper → 400
     * - UserAlreadyExistsException → DuplicateUserExceptionMapper → 409
     *
     * @param request DTO de entrada validado pelo Bean Validation
     * @param uriInfo contexto JAX-RS para construção da URI Location (RFC 7231)
     * @return 201 Created com UserResponse no body e Location header
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Criar novo usuário", description = "Cria um novo usuário no sistema. Retorna 409 se o email já estiver cadastrado.")
    @APIResponse(responseCode = "201",
            description = "Usuário criado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "400",
            description = "Dados de entrada inválidos (Bean Validation falhou)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409",
            description = "Email já cadastrado no sistema",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response create(
            @RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = CreateUserRequest.class),
                            examples = @ExampleObject(name = "valido",
                                    value = "{\"name\":\"Ana Lima\",\"email\":\"ana@fiap.br\",\"password\":\"abc123\"}")))
            @Valid CreateUserRequest request,
            @Context UriInfo uriInfo) {
        var command = userDtoMapper.toCommand(request);
        var user = createUserInputPort.execute(command);
        UserResponse response = userDtoMapper.toResponse(user);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }
}
