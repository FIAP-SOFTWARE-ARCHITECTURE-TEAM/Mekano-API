package com.fiap.mekano.application.service.admin;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateAdminUserCommand;
import com.fiap.mekano.domain.port.out.PasswordHasherPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.UserRoleRepositoryPort;
import com.fiap.mekano.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    UserRoleRepositoryPort userRoleRepository;

    @Mock
    PasswordHasherPort passwordHasher;

    AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService();
        service.userRepository = userRepository;
        service.userRoleRepository = userRoleRepository;
        service.passwordHasher = passwordHasher;
    }

    @Test
    void criarUsuario_comRoleAdmin_deveGerarSenhaPersistirUsuarioERole() {
        UUID userUuid = UUID.randomUUID();

        var command = new CreateAdminUserCommand(
                "Admin Novo",
                "ADMIN.NOVO@MEKANO.COM",
                Role.admin
        );

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(userUuid);
        when(savedUser.getName()).thenReturn("Admin Novo");

        when(userRepository.findByEmail("admin.novo@mekano.com"))
                .thenReturn(Optional.empty());

        when(passwordHasher.hash(anyString()))
                .thenReturn("$2a$hash");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        var result = service.criarUsuario(command);

        assertEquals(userUuid, result.id());
        assertEquals("Admin Novo", result.name());
        assertEquals("admin.novo@mekano.com", result.email());
        assertEquals(Role.admin, result.role());

        assertNotNull(result.generatedPassword());
        assertTrue(result.generatedPassword().length() >= 12);
        assertTrue(result.generatedPassword().matches(".*[A-Z].*"));
        assertTrue(result.generatedPassword().matches(".*[a-z].*"));
        assertTrue(result.generatedPassword().matches(".*\\d.*"));
        assertTrue(result.generatedPassword().matches(".*[!@#$%&*()\\-_=+\\[\\]{}].*"));

        verify(passwordHasher).hash(result.generatedPassword());
        verify(userRepository).save(any(User.class));
        verify(userRoleRepository).save(userUuid, Role.admin);
    }

    @Test
    void criarUsuario_comRoleCliente_deveAutoProvisionarUserRoleCliente() {
        UUID userUuid = UUID.randomUUID();

        var command = new CreateAdminUserCommand(
                "Cliente Novo",
                "cliente.novo@mekano.com",
                Role.cliente
        );

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(userUuid);
        when(savedUser.getName()).thenReturn("Cliente Novo");

        when(userRepository.findByEmail("cliente.novo@mekano.com"))
                .thenReturn(Optional.empty());

        when(passwordHasher.hash(anyString()))
                .thenReturn("$2a$hash");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        var result = service.criarUsuario(command);

        assertEquals(Role.cliente, result.role());
        verify(userRoleRepository).save(userUuid, Role.cliente);
    }

    @Test
    void criarUsuario_deveSalvarSenhaHasheadaENaoSenhaPlaintext() {
        UUID userUuid = UUID.randomUUID();

        var command = new CreateAdminUserCommand(
                "Admin Novo",
                "admin.novo@mekano.com",
                Role.admin
        );

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(userUuid);
        when(savedUser.getName()).thenReturn("Admin Novo");

        when(userRepository.findByEmail("admin.novo@mekano.com"))
                .thenReturn(Optional.empty());

        when(passwordHasher.hash(anyString()))
                .thenReturn("$2a$hash");

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        var result = service.criarUsuario(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User userToSave = userCaptor.getValue();

        assertEquals("$2a$hash", userToSave.getPasswordHash());
        assertFalse("$2a$hash".equals(result.generatedPassword()));
    }

    @Test
    void criarUsuario_quandoEmailJaExiste_deveLancar409ENaoCriarRole() {
        var command = new CreateAdminUserCommand(
                "Admin Existente",
                "admin.existente@mekano.com",
                Role.admin
        );

        User existingUser = mock(User.class);

        when(userRepository.findByEmail("admin.existente@mekano.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(AppException.class, () -> service.criarUsuario(command));

        verify(passwordHasher, never()).hash(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userRoleRepository, never()).save(any(UUID.class), any(Role.class));
    }

    @Test
    void criarUsuario_quandoRoleNula_deveLancar400() {
        var command = new CreateAdminUserCommand(
                "Admin Sem Role",
                "admin.sem.role@mekano.com",
                null
        );

        assertThrows(AppException.class, () -> service.criarUsuario(command));

        verify(userRepository, never()).save(any(User.class));
    }
}
