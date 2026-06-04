package com.fiap.mekano.application.usecase.user;

import com.fiap.mekano.domain.exception.InvalidUserDataException;
import com.fiap.mekano.domain.exception.UserAlreadyExistsException;
import com.fiap.mekano.domain.exception.UserNotFoundException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import com.fiap.mekano.domain.port.in.CreateUserInputPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Caso de uso de criação de usuário.
 *
 * Orquestra:
 * 1. Validação de dados de entrada (nome nulo/vazio)
 * 2. Verificação de duplicidade de email
 * 3. Hash BCrypt da senha (responsabilidade da camada application)
 * 4. Criação da entidade de domínio (User.create valida email via VO)
 * 5. Persistência via output port
 *
 * @ApplicationScoped: CDI é permitido na camada application (acoplamento consciente).
 * @Transactional: PROIBIDO aqui — transações são responsabilidade de infrastructure.
 */
@ApplicationScoped
public class CreateUserUseCase implements CreateUserInputPort {

    private final UserRepositoryPort userRepository;

    /**
     * Injeção por construtor — necessário para @InjectMocks do Mockito funcionar corretamente.
     */
    public CreateUserUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User execute(CreateUserCommand command) {
        // 1. Validação de nome — use case é responsável por esta guarda
        if (command.name() == null || command.name().isBlank()) {
            throw new InvalidUserDataException("O nome do usuário não pode ser nulo ou vazio");
        }

        // 2. Verificar duplicidade de email
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException(command.email());
        }

        // 3. Hash de senha — NÃO repassar senha raw para User.create()
        String passwordHash = BcryptUtil.bcryptHash(command.password());

        // 4. Criar entidade (Email VO valida formato — lança InvalidEmailException se inválido)
        User user = User.create(command.name(), command.email(), passwordHash);

        // 5. Persistir via output port
        return userRepository.save(user);
    }

    /**
     * Busca um usuário ativo pelo UUID.
     *
     * <p>Delega ao repositório que aplica o filtro de soft delete ({@code isActive = true}).
     * Se o usuário foi deletado logicamente, {@link UserNotFoundException} é lançada.
     *
     * @param id UUID do usuário
     * @return User encontrado e ativo
     * @throws UserNotFoundException se o UUID não existir ou não estiver ativo
     */
    @Override
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * Exclui logicamente um usuário (soft delete).
     *
     * <p>Marca o registro como {@code isActive = false} e registra o timestamp
     * de exclusão ({@code deletedAt}). O registro NÃO é removido fisicamente.
     *
     * @param id UUID do usuário a excluir
     * @throws UserNotFoundException se o UUID não existir
     */
    @Override
    public void deleteUser(UUID id) {
        userRepository.markAsDeleted(id);
    }
}
