package com.fiap.mekano.application.service.user;

import com.fiap.mekano.domain.event.UserCreatedEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import com.fiap.mekano.domain.port.in.PasswordHasher;
import com.fiap.mekano.domain.port.in.UserServicePort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.PasswordHasherPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserService implements UserServicePort {

    private final UserRepositoryPort userRepository;

    private final PasswordHasherPort passwordHasher;

    private final EventPublisher eventPublisher;

    public UserService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public User execute(CreateUserCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw new AppException(400, Messages.get("user.name.required"));
        }

        if (userRepository.existsByEmail(command.email())) {
            throw new AppException(409, Messages.get("user.already.exists", command.email()));
        }

        String passwordHash = passwordHasher.hash(command.password());

        User user = User.create(command.name(), command.email(), command.active(), passwordHash);

        User savedUser = userRepository.save(user);

        eventPublisher.publish(UserCreatedEvent.of(savedUser));

        return savedUser;
    }

    public CreateUserResponse executeResponse(CreateUserCommand command) {
        User user = execute(command);
        return new CreateUserResponse(user.getId(), user.getName(), user.getEmail().getValue(), user.getCreatedAt());
    }

    @Override
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("user.not.found", id)));
    }

    @Override
    public List<User> findAllUsers(int page, int size, String sort) {
        return userRepository.findAll(page, size, sort);
    }

    @Override
    public long countAllUsers() {
        return userRepository.countAll();
    }

    @Override
    public void deleteUser(UUID id) {
        userRepository.markAsDeleted(id);
    }
}
