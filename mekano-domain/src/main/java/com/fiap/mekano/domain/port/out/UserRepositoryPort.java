package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — contrato de persistência de usuários.
 *
 * Esta interface é definida no domínio e implementada pelo módulo infrastructure.
 * O domínio não conhece JPA, Panache ou qualquer tecnologia de banco de dados.
 *
 * Implementação concreta: UserRepository em mekano-infrastructure (Fase 4).
 */
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAll(int page, int size, String sort);

    long countAll();

    /**
     * Marca um usuário como deletado (soft delete).
     *
     * Define deleted_at e is_active=false. O registro permanece no banco
     * mas é excluído de todas as queries de listagem/busca padrão.
     *
     * @param id UUID do usuário a marcar como deletado
     */
    void markAsDeleted(UUID id);
    
    void softDelete(UUID uuid);
}
