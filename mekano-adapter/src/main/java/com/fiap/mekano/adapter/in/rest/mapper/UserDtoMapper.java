package com.fiap.mekano.adapter.in.rest.mapper;

import com.fiap.mekano.adapter.in.rest.dto.CreateUserRequest;
import com.fiap.mekano.adapter.in.rest.dto.UserResponse;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct entre DTOs HTTP e tipos de domínio.
 *
 * <p>Reabilitado na Phase 9 (09-05) após isolamento do bug
 * quarkus-maven-plugin generate-code. Estratégia: {@code componentModel = "cdi"}.
 *
 * <p>Mapeamentos:
 * <ul>
 *   <li>{@link #toCommand(CreateUserRequest)}: name, email, password — todos String 1-para-1</li>
 *   <li>{@link #toResponse(User)}: User.email (Email VO) → UserResponse.email (String)
 *       via expressão {@code user.getEmail().getValue()}; passwordHash NÃO incluído no DTO</li>
 * </ul>
 */
@Mapper(componentModel = "cdi")
public interface UserDtoMapper {

    /**
     * Converte DTO HTTP de entrada para comando de domínio.
     *
     * <p>Mapeamento direto: todos os campos têm mesmo nome e tipo (String → String).
     *
     * @param request DTO validado por Bean Validation no resource
     * @return comando pronto para {@code CreateUserInputPort.execute()}
     */
    CreateUserCommand toCommand(CreateUserRequest request);

    /**
     * Converte entidade de domínio para DTO HTTP de saída.
     *
     * <p>{@code passwordHash} fica ausente do {@link UserResponse} — o record não tem
     * o campo, portanto o hash nunca é exposto na resposta HTTP.
     *
     * @param user entidade retornada pelo use case
     * @return DTO de saída sem passwordHash
     */
    @Mapping(target = "email", expression = "java(user.getEmail().getValue())")
    UserResponse toResponse(User user);
}
