package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.User;

import java.util.UUID;

/**
 * Input port — contrato dos serviços de gerenciamento de usuário.
 *
 * Interface definida no domínio e implementada pelo módulo application.
 */
public interface UserServicePort {

    User execute(CreateUserCommand command);

    User findUserById(UUID id);

    void deleteUser(UUID id);
}
