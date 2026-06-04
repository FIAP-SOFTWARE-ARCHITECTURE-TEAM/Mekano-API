package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.User;

import java.util.UUID;

/**
 * Input port — contrato dos casos de uso de gerenciamento de usuário.
 *
 * Esta interface é definida no domínio e implementada pelo módulo application.
 * O adapter chama este port para iniciar os casos de uso.
 *
 * Implementação concreta: CreateUserUseCase em mekano-application (Fase 3).
 */
public interface CreateUserInputPort {

    /**
     * Executa o caso de uso de criação de usuário.
     *
     * @param command objeto contendo name, email (raw) e password (raw)
     * @return User criado e persistido
     */
    User execute(CreateUserCommand command);

    /**
     * Busca um usuário ativo pelo UUID.
     *
     * @param id UUID do usuário
     * @return User encontrado
     * @throws com.fiap.mekano.domain.exception.UserNotFoundException se o UUID não existir ou estiver deletado
     */
    User findUserById(UUID id);

    /**
     * Exclui logicamente um usuário (soft delete).
     *
     * @param id UUID do usuário a excluir
     * @throws com.fiap.mekano.domain.exception.UserNotFoundException se o UUID não existir
     */
    void deleteUser(UUID id);
}
