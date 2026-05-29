package com.fiap.mekano.adapter.in.rest.mapper;

import com.fiap.mekano.adapter.in.rest.dto.CreateUserRequest;
import com.fiap.mekano.adapter.in.rest.dto.UserResponse;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper CDI para conversão entre DTOs HTTP e tipos de domínio.
 *
 * <p>componentModel = "cdi": bean CDI gerenciado pelo Quarkus Arc (STATE.md Decision 4).
 * NUNCA usar "spring" neste projeto.
 *
 * <p>Mapeamentos:
 * <ul>
 *   <li>{@code toCommand()}: auto-mapeado (name, email, password coincidem em nome e tipo)</li>
 *   <li>{@code toResponse()}: requer @Mapping para email — User.email é Email VO, UserResponse.email é String</li>
 * </ul>
 *
 * <p>MapStruct 1.6.x com record como TARGET: usa o canonical constructor automaticamente.
 * passwordHash não é campo de UserResponse — MapStruct ignora automaticamente (sem @Mapping necessário).
 */
@Mapper(componentModel = "cdi")
public interface UserDtoMapper {

    /**
     * Converte DTO de entrada HTTP para comando de domínio.
     *
     * <p>Auto-mapeamento: CreateUserRequest.name → CreateUserCommand.name,
     *                     CreateUserRequest.email → CreateUserCommand.email,
     *                     CreateUserRequest.password → CreateUserCommand.password.
     * Nenhum @Mapping necessário — nomes e tipos coincidem exatamente.
     *
     * @param request DTO de entrada validado pelo Bean Validation (@Valid no resource)
     * @return comando de domínio pronto para CreateUserInputPort.execute()
     */
    CreateUserCommand toCommand(CreateUserRequest request);

    /**
     * Converte entidade de domínio para DTO de saída HTTP.
     *
     * <p>Email VO → String: usa expression Java diretamente (mesmo padrão de UserEntityMapper).
     * passwordHash ausente de UserResponse: MapStruct ignora campos sem correspondência no target.
     * MapStruct 1.6.x chama UserResponse(UUID, String, String, LocalDateTime) canonical constructor.
     *
     * @param user entidade de domínio retornada pelo use case
     * @return DTO de saída sem passwordHash
     */
    @Mapping(target = "email", expression = "java(user.getEmail().getValue())")
    UserResponse toResponse(User user);
}
