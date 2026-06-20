package com.fiap.mekano.rest.api;

import com.fiap.mekano.rest.api.dto.CreateUserRequest;
import com.fiap.mekano.rest.api.dto.UserPageResponse;
import com.fiap.mekano.rest.api.dto.UserResponse;
import com.fiap.mekano.rest.api.exception.ErrorResponse;
import com.fiap.mekano.rest.api.mapper.UserDtoMapper;
import com.fiap.mekano.application.usecase.user.CreateUserUseCase;
import com.fiap.mekano.domain.exception.UserAlreadyExistsException;
import com.fiap.mekano.domain.exception.UserNotFoundException;
import com.fiap.mekano.domain.port.in.CreateUserInputPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
import java.util.UUID;

/**
 * JAX-RS Resource para operações de usuário.
 *
 * Responsabilidades:
 * - Receber requisições HTTP POST /users
 * - Delegar validação ao Bean Validation (@Valid)
 * - Converter DTO → Command → User → DTO via UserDtoMapper
 * - Invocar CreateUserInputPort (nunca CreateUserUseCase diretamente — INH-04)
 * - Construir resposta 201 Created com Location header (D-01)
 * - Exige autenticação JWT em todos os endpoints (`@Authenticated`, D-01).
 * - POST /users requer role `user` (`@RolesAllowed`, D-01).
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

    /**
     * Exemplo de password usado em {@code @APIResponse}/{@code @ExampleObject}.
     * Mantido como constante para evitar drift entre o exemplo OpenAPI e os
     * literais usados nos testes (IN-03 — Code Review 08).
     */
    static final String EXAMPLE_PASSWORD = "abc123";

    @Inject
    CreateUserInputPort createUserInputPort;

    @Inject
    CreateUserUseCase createUserUseCase;

    @Inject
    UserDtoMapper userDtoMapper;

    @Inject
    UserRepositoryPort userRepositoryPort;

    /**
     * Cria um novo usuário no sistema.
     *
     * Fluxo:
     * 1. Bean Validation valida CreateUserRequest (@Valid)
     * 2. UserDtoMapper converte request → CreateUserCommand
     * 3. CreateUserUseCase.executeResponse() orquestra: verifica duplicidade → hash via PasswordHasher → User.create() → save()
     * 4. UserDtoMapper converte CreateUserResponse → UserResponse
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
                                    value = "{\"name\":\"Ana Lima\",\"email\":\"ana@fiap.br\",\"password\":\"" + EXAMPLE_PASSWORD + "\"}")))
            @Valid CreateUserRequest request,
            @Context UriInfo uriInfo) throws UserAlreadyExistsException {
        var command = userDtoMapper.toCommand(request);
        var response = createUserUseCase.executeResponse(command);
        UserResponse userResponse = userDtoMapper.toResponse(response);
        URI location = uriInfo.getAbsolutePathBuilder().path(userResponse.id().toString()).build();
        return Response.created(location).entity(userResponse).build();
    }

    /**
     * Lista todos os usuários ativos de forma paginada.
     *
     * <p>A paginação é 0-based: page=0 retorna a primeira página.
     * A ordenação padrão é por nome ascendente.
     *
     * @param page número da página (0-based, default 0)
     * @param size tamanho da página (default 10)
     * @param sort campo e direção de ordenação (ex: "name,asc", default "name,asc")
     * @return 200 OK com UserPageResponse contendo lista paginada
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar usuários", description = "Retorna usuários ativos de forma paginada e ordenada")
    @APIResponse(responseCode = "200",
            description = "Lista paginada de usuários",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UserPageResponse.class)))
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("name,asc") String sort) {
        var content = userRepositoryPort.findAll(page, size, sort)
                .stream()
                .map(userDtoMapper::toResponse)
                .toList();
        long total = userRepositoryPort.countAll();
        int totalPages = (int) Math.ceil((double) total / size);
        var response = new UserPageResponse(content, page, size, total, totalPages);
        return Response.ok(response).build();
    }

    /**
     * Busca um usuário ativo pelo UUID.
     *
     * <p>Usuários deletados logicamente (soft delete) retornam 404.
     *
     * @param id UUID do usuário
     * @return 200 OK com UserResponse no body
     * @throws com.fiap.mekano.domain.exception.UserNotFoundException se não encontrado ou inativo
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados do usuário ativo. Usuários deletados (soft delete) retornam 404.")
    @APIResponse(responseCode = "200",
            description = "Usuário encontrado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "404",
            description = "Usuário não encontrado ou deletado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response getById(@PathParam("id") UUID id) throws UserNotFoundException {
        var user = createUserInputPort.findUserById(id);
        UserResponse response = userDtoMapper.toResponse(user);
        return Response.ok(response).build();
    }

    /**
     * Exclui (soft delete) um usuário pelo UUID.
     *
     * <p>Marca o registro como inativo (is_active=false) e registra o timestamp
     * de exclusão (deleted_at). O registro NÃO é removido fisicamente.
     *
     * @param id UUID do usuário a excluir
     * @return 204 No Content (sem corpo)
     * @throws com.fiap.mekano.domain.exception.UserNotFoundException se o UUID não existir
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir usuário", description = "Marca o usuário como inativo (soft delete). Retorna 204 se bem-sucedido.")
    @APIResponse(responseCode = "204", description = "Usuário excluído com sucesso (soft delete)")
    @APIResponse(responseCode = "404",
            description = "Usuário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response delete(@PathParam("id") UUID id) throws UserNotFoundException {
        createUserInputPort.deleteUser(id);
        return Response.noContent().build();
    }
}
