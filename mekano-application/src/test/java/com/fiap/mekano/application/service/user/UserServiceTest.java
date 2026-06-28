package com.fiap.mekano.application.service.user;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.PasswordHasherPort;
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
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    PasswordHasherPort passwordHasher;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("deve criar usuário com dados válidos")
    void deveCriarUsuarioComDadosValidos() {
        var command = new CreateUserCommand("João Silva", "joao@fiap.br",true, "senha123");
        when(userRepository.existsByEmail("joao@fiap.br")).thenReturn(false);
        when(passwordHasher.hash("senha123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.execute(command);

        assertNotNull(result);
        assertEquals("João Silva", result.getName());
        assertEquals("joao@fiap.br", result.getEmail().getValue());
        verify(userRepository, times(1)).save(any(User.class));
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("deve lançar AppException(409) quando email duplicado")
    void deveLancarExcecaoQuandoEmailDuplicado() {
        var command = new CreateUserCommand("João Silva", "joao@fiap.br",true, "senha123");
        when(userRepository.existsByEmail("joao@fiap.br")).thenReturn(true);

        assertThrows(AppException.class, () -> userService.execute(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar AppException(400) quando email é inválido")
    void devePropagardInvalidEmailExceptionQuandoEmailInvalido() {
        var command = new CreateUserCommand("João Silva", "email-invalido",false, "senha123");
        when(userRepository.existsByEmail("email-invalido")).thenReturn(false);

        assertThrows(AppException.class, () -> userService.execute(command));
    }

    @Test
    @DisplayName("deve lançar AppException(400) quando nome é nulo")
    void deveLancarExcecaoQuandoNomeNulo() {
        var command = new CreateUserCommand(null, "joao@fiap.br",false, "senha123");

        assertThrows(AppException.class, () -> userService.execute(command));
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }
}
