package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Bean CDI Panache que expõe as operações de banco para {@link UserEntity}.
 *
 * <p>Separado de {@link UserRepositoryImpl} para evitar conflito de assinatura:
 * {@code PanacheRepositoryBase.findById(UUID)} retorna {@code UserEntity},
 * enquanto {@code UserRepositoryPort.findById(UUID)} retorna {@code Optional<User>}.
 * Java não permite dois métodos com a mesma assinatura e tipos de retorno incompatíveis
 * na mesma classe. A separação em dois beans resolve o conflito sem sacrificar
 * nenhum dos contratos.
 */
@ApplicationScoped
public class UserPanacheRepository implements PanacheRepositoryBase<UserEntity, UUID> {
    // Todos os métodos Panache (persist, flush, findByIdOptional, find, count, etc.)
    // são herdados de PanacheRepositoryBase via bytecode enhancement do Quarkus.
}
