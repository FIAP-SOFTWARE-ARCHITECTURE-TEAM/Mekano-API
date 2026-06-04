package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.exception.UserNotFoundException;
import com.fiap.mekano.domain.model.User;

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

    /**
     * Persiste um usuário novo ou atualiza um existente.
     *
     * @param user entidade a ser salva
     * @return entidade salva (pode conter dados atualizados pela persistence layer)
     */
    User save(User user);

    /**
     * Busca um usuário pelo identificador único.
     *
     * @param id UUID do usuário
     * @return Optional contendo o usuário se encontrado, ou Optional.empty()
     */
    Optional<User> findById(UUID id);

    /**
     * Busca um usuário pelo endereço de email.
     *
     * @param email endereço de email (string normalizada)
     * @return Optional contendo o usuário se encontrado, ou Optional.empty()
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se já existe um usuário cadastrado com o email informado.
     * Mais eficiente que findByEmail() quando só há necessidade de verificar existência.
     *
     * @param email endereço de email a verificar
     * @return true se existe, false caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Marca um usuário como deletado (soft delete).
     *
     * Define deleted_at e is_active=false. O registro permanece no banco
     * mas é excluído de todas as queries de listagem/busca padrão.
     *
     * @param id UUID do usuário a marcar como deletado
     * @throws com.fiap.mekano.domain.exception.UserNotFoundException se o UUID não existir
     */
    void markAsDeleted(UUID id) throws UserNotFoundException;
}
