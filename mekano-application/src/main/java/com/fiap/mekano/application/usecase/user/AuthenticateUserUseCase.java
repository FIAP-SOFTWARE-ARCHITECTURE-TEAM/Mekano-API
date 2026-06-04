package com.fiap.mekano.application.usecase.user;

import com.fiap.mekano.domain.exception.InvalidCredentialsException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.AuthenticateUserCommand;
import com.fiap.mekano.domain.port.in.AuthenticateUserInputPort;
import com.fiap.mekano.domain.port.in.PasswordHasher;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Caso de uso de autenticação de usuário.
 *
 * Orquestra:
 * 1. Lookup do usuário por email via {@link UserRepositoryPort}.
 * 2. Comparação da senha plaintext com o hash armazenado
 *    via {@link PasswordHasher#matches(String, String)}.
 * 3. Devolve o {@link User} autenticado para o adapter emitir o JWT.
 *
 * <p>T-08-07 / Information Disclosure: <b>email inexistente</b> e
 * <b>senha incorreta</b> são colapsados em uma única
 * {@link InvalidCredentialsException} para evitar user enumeration.
 *
 * @ApplicationScoped: CDI permitido na camada application.
 * @Transactional: PROIBIDO aqui — read-only por natureza, transações
 * são responsabilidade de infrastructure (INH-06).
 */
@ApplicationScoped
public class AuthenticateUserUseCase implements AuthenticateUserInputPort {

    private final UserRepositoryPort userRepository;

    private final PasswordHasher passwordHasher;

    public AuthenticateUserUseCase(UserRepositoryPort userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User execute(AuthenticateUserCommand command) throws InvalidCredentialsException {
        if (command == null || command.email() == null || command.password() == null) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}
