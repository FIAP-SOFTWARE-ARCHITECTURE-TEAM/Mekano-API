package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.infrastructure.mapper.UserEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação concreta de {@link UserRepositoryPort} usando Quarkus Panache.
 *
 * <p>Padrão de duas classes para evitar conflito de assinatura:
 * {@code PanacheRepositoryBase.findById(UUID)} retorna {@code UserEntity},
 * enquanto {@code UserRepositoryPort.findById(UUID)} retorna {@code Optional<User>}.
 * Por isso, a herança Panache fica em {@link UserPanacheRepository} (bean separado),
 * e esta classe delega a ela via injeção, implementando apenas o contrato de domínio.
 *
 * <p>Regras de transação:
 * <ul>
 *   <li>{@code save()} — {@code @Transactional}: garante que persist()+flush() executam em uma
 *       transação e que constraint violations são capturadas imediatamente (D-02)</li>
 *   <li>Métodos de leitura — sem {@code @Transactional}: Quarkus/JPA gerencia automaticamente
 *       se houver transação ativa no chamador</li>
 * </ul>
 */
@ApplicationScoped
public class UserRepositoryImpl implements UserRepositoryPort {

    @Inject
    UserPanacheRepository panacheRepository;

    @Inject
    UserEntityMapper mapper;

    /**
     * Persiste um novo usuário ou atualiza um existente.
     *
     * <p>{@code flush()} após {@code persist()} garante execução imediata do INSERT,
     * capturando violações de constraint (ex: email duplicado) dentro da transação
     * antes de retornar ao chamador (D-02).
     *
     * @param user entidade de domínio a ser salva
     * @return entidade de domínio reconstruída após persistência
     */
    @Override
    @Transactional
    public User save(User user) {
        var entity = mapper.toEntity(user);
        panacheRepository.persist(entity);
        panacheRepository.flush();
        return mapper.toDomain(entity);
    }

    /**
     * Busca usuário pelo UUID.
     *
     * @param id UUID do usuário
     * @return Optional com o User se encontrado, ou Optional.empty()
     */
    @Override
    public Optional<User> findById(UUID id) {
        return panacheRepository.findByIdOptional(id).map(mapper::toDomain);
    }

    /**
     * Busca usuário pelo email.
     *
     * <p>Usa Panache HQL abreviado: {@code find("email", email)} equivale a
     * {@code SELECT u FROM UserEntity u WHERE u.email = ?1}.
     *
     * @param email string do email (normalizada)
     * @return Optional com o User se encontrado, ou Optional.empty()
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return panacheRepository.find("email", email).firstResultOptional().map(mapper::toDomain);
    }

    /**
     * Verifica existência de usuário pelo email usando COUNT.
     *
     * <p>Mais eficiente que {@code findByEmail().isPresent()} — não carrega a entidade completa.
     *
     * @param email string do email
     * @return true se existe usuário com esse email, false caso contrário
     */
    @Override
    public boolean existsByEmail(String email) {
        return panacheRepository.count("email", email) > 0;
    }
}
