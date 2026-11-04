package com.fiap.mekano.application.service.admin;

import com.fiap.mekano.application.service.auth.PasswordGenerator;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.AdminCreatedUser;
import com.fiap.mekano.domain.port.in.AdminUserServicePort;
import com.fiap.mekano.domain.port.in.AdminUserSummary;
import com.fiap.mekano.domain.port.in.CreateAdminUserCommand;
import com.fiap.mekano.domain.port.out.PasswordHasherPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.UserRoleRepositoryPort;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AdminUserService implements AdminUserServicePort {

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    UserRoleRepositoryPort userRoleRepository;

    @Inject
    PasswordHasherPort passwordHasher;

    @Override
    @Transactional
    public AdminCreatedUser criarUsuario(CreateAdminUserCommand command) {
        validar(command);

        String email = command.email().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new AppException(409, Messages.get("user.already.exists", email));
        }

        String generatedPassword = PasswordGenerator.generate(12);
        String passwordHash = passwordHasher.hash(generatedPassword);

       
        User user = User.create(
                command.name().trim(),
                email,
                true,
                passwordHash
        );
        
        User savedUser = userRepository.save(user);

        userRoleRepository.save(savedUser.getId(), command.role());

        return new AdminCreatedUser(
                savedUser.getId(),
                savedUser.getName(),
                email,
                command.role(),
                generatedPassword
        );
    }

    @Override
    public List<AdminUserSummary> listar(int page, int size, Boolean isActive) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return userRepository.findAll(safePage, safeSize, "createdAt desc", isActive)
                .stream()
                .map(user -> new AdminUserSummary(
                        user.getId(),
                        user.getName(),
                        user.getEmail().getValue(),
                        userRoleRepository.findRoleByUserUuid(user.getId()).orElse(null),
                        user.isActive()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void deletar(UUID uuid) {
        if (uuid == null) {
            throw new AppException(400, Messages.get("admin.uuid.required"));
        }

        userRepository.softDelete(uuid);
    }

    private void validar(CreateAdminUserCommand command) {
        if (command == null) {
            throw new AppException(400, Messages.get("admin.requisicao.invalida"));
        }

        if (command.name() == null || command.name().isBlank()) {
            throw new AppException(400, Messages.get("admin.nome.required"));
        }

        if (command.email() == null || command.email().isBlank()) {
            throw new AppException(400, Messages.get("admin.email.required"));
        }

        if (command.role() == null) {
            throw new AppException(400, Messages.get("admin.role.required"));
        }
    }
}