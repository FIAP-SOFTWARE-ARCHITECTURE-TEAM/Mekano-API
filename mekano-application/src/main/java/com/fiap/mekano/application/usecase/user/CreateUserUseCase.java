package com.fiap.mekano.application.usecase.user;

import com.fiap.mekano.domain.event.UserCreatedEvent;
import com.fiap.mekano.domain.exception.InvalidUserDataException;
import com.fiap.mekano.domain.exception.UserAlreadyExistsException;
import com.fiap.mekano.domain.exception.UserNotFoundException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import com.fiap.mekano.domain.port.in.CreateUserInputPort;
import com.fiap.mekano.domain.port.in.PasswordHasher;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

/**
 * Caso de uso de criação de usuário.
 *
 * Orquestra:
 * 1. Validação de dados de entrada (nome nulo/vazio)
 * 2. Verificação de duplicidade de email
 * 3. Hash da senha via {@link PasswordHasher} (abstração injetada)
 * 4. Criação da entidade de domínio (User.create valida email via VO)
 * 5. Persistência via output port
 *
 * @ApplicationScoped: CDI é permitido na camada application (acoplamento consciente).
 * @Transactional no método execute(): a unidade de trabalho pertence ao use case (D-03),
 * não ao repositório — garante que persist()+flush() executam em uma transação.
 */
@ApplicationScoped
public class CreateUserUseCase implements CreateUserInputPort {

    private final UserRepositoryPort userRepository;

    private final PasswordHasher passwordHasher;

    private final EventPublisher eventPublisher;

    /**
     * Injeção por construtor — necessário para @InjectMocks do Mockito funcionar corretamente.
     */
    public CreateUserUseCase(UserRepositoryPort userRepository, PasswordHasher passwordHasher, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
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
        String passwordHash = passwordHasher.hash(command.password());

        // 4. Criar entidade (Email VO valida formato — lança InvalidEmailException se inválido)
        User user = User.create(command.name(), command.email(), passwordHash);

        // 5. Persistir via output port
        User savedUser = userRepository.save(user);

        // 6. Publicar evento de domínio — notifica listeners sobre criação do usuário
        eventPublisher.publish(UserCreatedEvent.of(savedUser));

        return savedUser;
    }

    /**
     * Executa o caso de uso de criação e retorna um {@link CreateUserResponse}.
     *
     * <p>Diferença em relação a {@link #execute(CreateUserCommand)}: retorna um
     * record de resposta ({@code id/name/email/createdAt}) em vez da entidade
     * {@code User} diretamente (D-04).
     *
     * @param command comando de criação
     * @return resposta com dados públicos do usuário criado
     */
    public CreateUserResponse executeResponse(CreateUserCommand command) {
        User user = execute(command);
        return new CreateUserResponse(user.getId(), user.getName(), user.getEmail().getValue(), user.getCreatedAt());
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
