package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.infrastructure.entity.UserEntity;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepositoryPort {

    private final UserPanacheRepository panacheRepository;

    public UserRepositoryImpl(UserPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = panacheRepository.find("uuid = ?1", user.getId()).firstResult();
        if (entity == null) {
            entity = new UserEntity();
        }

        entity.setUuid(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail().getValue());
        entity.setPasswordHash(user.getPasswordHash());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(user.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(UserRepositoryImpl::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        return panacheRepository.find("email = ?1 and isActive = ?2", normalizedEmail, true)
                .firstResultOptional()
                .map(UserRepositoryImpl::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        return panacheRepository.count("email = ?1 and isActive = ?2", normalizedEmail, true) > 0;
    }

    @Override
    public List<User> findAll(int page, int size, String sort) {
        return panacheRepository.find("isActive = ?1", parseSort(sort), true)
                .page(Page.of(Math.max(page, 0), normalizeSize(size)))
                .list()
                .stream()
                .map(UserRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive = ?1", true);
    }

    @Override
    public void markAsDeleted(UUID id) {
        panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .ifPresent(entity -> {
                    entity.setIsActive(false);
                    entity.setDeletedAt(LocalDateTime.now());
                });
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private static Sort parseSort(String sort) {
        String sortValue = sort == null || sort.isBlank() ? "name,asc" : sort;
        String[] sortParts = sortValue.split(",", 2);

        String sortField = switch (sortParts[0].strip()) {
            case "name", "email", "createdAt" -> sortParts[0].strip();
            default -> "name";
        };

        boolean ascending = sortParts.length < 2 || !"desc".equalsIgnoreCase(sortParts[1].strip());
        return ascending ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
    }

    private static User toDomain(UserEntity entity) {
        return User.reconstitute(
                entity.getUuid(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }
}
