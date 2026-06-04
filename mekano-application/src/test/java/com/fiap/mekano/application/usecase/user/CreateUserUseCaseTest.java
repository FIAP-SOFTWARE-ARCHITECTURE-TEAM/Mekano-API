package com.fiap.mekano.application.usecase.user;

import com.fiap.mekano.domain.exception.InvalidEmailException;
import com.fiap.mekano.domain.exception.InvalidUserDataException;
import com.fiap.mekano.domain.exception.UserAlreadyExistsException;
import com.fiap.mekano.domain.exception.UserNotFoundException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import com.fiap.mekano.domain.port.in.PasswordHasher;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserUseCase")
class CreateUserUseCaseTest {

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    CreateUserUseCase useCase;

    @Test
    @DisplayName("deve criar usuário com dados válidos")
    void deveCriarUsuarioComDadosValidos() throws UserAlreadyExistsException {
        // Arrange
        var command = new CreateUserCommand("João Silva", "joao@fiap.br", "senha123");
        when(userRepository.existsByEmail("joao@fiap.br")).thenReturn(false);
        when(passwordHasher.hash("senha123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = useCase.execute(command);

        // Assert
        assertNotNull(result);
        assertEquals("João Silva", result.getName());
        assertEquals("joao@fiap.br", result.getEmail().getValue());
        verify(userRepository, times(1)).save(any(User.class));
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("deve lançar UserAlreadyExistsException quando email duplicado")
    void deveLancarExcecaoQuandoEmailDuplicado() {
        // Arrange
        var command = new CreateUserCommand("João Silva", "joao@fiap.br", "senha123");
        when(userRepository.existsByEmail("joao@fiap.br")).thenReturn(true);

        // Act + Assert
        assertThrows(UserAlreadyExistsException.class, () -> useCase.execute(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve propagar InvalidEmailException quando email é inválido")
    void devePropagardInvalidEmailExceptionQuandoEmailInvalido() {
        // Arrange — email inválido passado; existsByEmail retorna false (não há duplicata)
        var command = new CreateUserCommand("João Silva", "email-invalido", "senha123");
        when(userRepository.existsByEmail("email-invalido")).thenReturn(false);

        // Act + Assert — InvalidEmailException lançada por User.create() via Email VO
        assertThrows(InvalidEmailException.class, () -> useCase.execute(command));
    }

    @Test
    @DisplayName("deve lançar InvalidUserDataException quando nome é nulo")
    void deveLancarExcecaoQuandoNomeNulo() {
        // Arrange — nome nulo; use case lança antes de chamar o repositório
        var command = new CreateUserCommand(null, "joao@fiap.br", "senha123");

        // Act + Assert
        assertThrows(InvalidUserDataException.class, () -> useCase.execute(command));
        // O repositório NÃO deve ser consultado quando o nome é inválido
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }
}
