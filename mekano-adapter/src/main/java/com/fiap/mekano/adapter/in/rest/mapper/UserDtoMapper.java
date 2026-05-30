package com.fiap.mekano.adapter.in.rest.mapper;

import com.fiap.mekano.adapter.in.rest.dto.CreateUserRequest;
import com.fiap.mekano.adapter.in.rest.dto.UserResponse;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper CDI manual entre DTOs HTTP e tipos de domínio.
 *
 * <p>Phase 8 (08-05 deviation, Rule 3): originalmente declarado como
 * {@code @Mapper(componentModel = "cdi")} interface gerada por MapStruct.
 * Em Quarkus 3.36 + maven-compiler-plugin 3.15 + quarkus-maven-plugin
 * (extensions=true), a segunda invocação do ciclo `generate-sources` (e.g.
 * surefire/quarkus:generate-code-tests) reescreve
 * {@code target/classes/UserDtoMapperImpl.class} em modo degradado
 * ("Unresolved compilation problems" sem `implements UserDtoMapper`),
 * fazendo o ArC do Quarkus emitir {@code UnsatisfiedResolutionException}
 * para {@code UserResource#userDtoMapper} em qualquer @QuarkusTest.
 *
 * <p>Solução: classe concreta @ApplicationScoped escrita à mão. Mesma
 * semântica dos métodos gerados por MapStruct (auto-mapeamento de fields
 * coincidentes; `email` extraído via {@code user.getEmail().getValue()}).
 *
 * <p>Mapeamentos:
 * <ul>
 *   <li>{@link #toCommand(CreateUserRequest)}: name, email, password (todos String 1-para-1)</li>
 *   <li>{@link #toResponse(User)}: User.email (Email VO) → UserResponse.email (String);
 *       passwordHash NÃO incluído no DTO (T-08-XX, segurança).</li>
 * </ul>
 */
@ApplicationScoped
public class UserDtoMapper {

    /**
     * Converte DTO HTTP de entrada para comando de domínio.
     *
     * <p>Auto-mapeamento direto: todos os fields têm mesmo nome e tipo.
     *
     * @param request DTO validado por Bean Validation no resource
     * @return comando pronto para CreateUserInputPort.execute()
     */
    public CreateUserCommand toCommand(CreateUserRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateUserCommand(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );
    }

    /**
     * Converte entidade de domínio para DTO HTTP de saída.
     *
     * <p>{@code passwordHash} ausente do {@link UserResponse} canonical
     * constructor → não é exposto na resposta (D-06 / segurança).
     *
     * @param user entidade retornada pelo use case
     * @return DTO de saída sem passwordHash
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail().getValue(),
                user.getCreatedAt()
        );
    }
}

