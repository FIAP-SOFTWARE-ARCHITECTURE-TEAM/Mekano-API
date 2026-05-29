package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.User;

/**
 * Input port — contrato do caso de uso de criação de usuário.
 *
 * Esta interface é definida no domínio e implementada pelo módulo application.
 * O adapter chama este port para iniciar o caso de uso.
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
}
