package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Role;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("UserRoleRepositoryImpl")
class UserRoleRepositoryImplTest {

    @Inject
    UserRoleRepositoryImpl repository;

    @Test
    @TestTransaction
    @DisplayName("findRoleByUserUuid deve retornar role when found")
    void findRoleByUserUuidDeveRetornarQuandoEncontrado() {
        UUID userUuid = UUID.randomUUID();
        repository.save(userUuid, Role.mecanico);

        Optional<Role> found = repository.findRoleByUserUuid(userUuid);

        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(Role.mecanico);
    }

    @Test
    @TestTransaction
    @DisplayName("findRoleByUserUuid deve retornar vazio when not found")
    void findRoleByUserUuidDeveRetornarVazioQuandoNaoEncontrado() {
        Optional<Role> found = repository.findRoleByUserUuid(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("save deve persistir nova role")
    void saveDevePersistirNovaRole() {
        UUID userUuid = UUID.randomUUID();

        repository.save(userUuid, Role.admin);

        Optional<Role> found = repository.findRoleByUserUuid(userUuid);
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(Role.admin);
    }

    @Test
    @TestTransaction
    @DisplayName("save não deve duplicar role já existente")
    void saveNaoDeveDuplicarRoleExistente() {
        UUID userUuid = UUID.randomUUID();
        repository.save(userUuid, Role.almoxarife);

        repository.save(userUuid, Role.almoxarife);

        Optional<Role> found = repository.findRoleByUserUuid(userUuid);
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(Role.almoxarife);
    }
}
