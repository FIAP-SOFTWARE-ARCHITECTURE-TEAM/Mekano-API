package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.exception.InvalidCredentialsException;
import com.fiap.mekano.domain.model.User;

/**
 * Input port — contrato do caso de uso de autenticação de usuário.
 *
 * Esta interface é definida no domínio e implementada pelo módulo application.
 * O adapter chama este port para validar credenciais antes de emitir um JWT.
 *
 * Implementação concreta: {@code AuthenticateUserUseCase} em mekano-application.
 */
public interface AuthenticateUserInputPort {

    /**
     * Valida as credenciais informadas e devolve o {@link User} autenticado.
     *
     * @param command objeto contendo email e password (raw)
     * @return User correspondente às credenciais válidas
     * @throws com.fiap.mekano.domain.exception.InvalidCredentialsException
     *         se o email não existir OU a senha não conferir (mensagem
     *         única para evitar user enumeration — T-08-07).
     */
    User execute(AuthenticateUserCommand command) throws InvalidCredentialsException;
}
