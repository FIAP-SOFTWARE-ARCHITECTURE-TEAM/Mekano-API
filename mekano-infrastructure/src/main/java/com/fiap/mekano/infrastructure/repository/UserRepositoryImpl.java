package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.UserNotFoundException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.infrastructure.entity.UserEntity;
import com.fiap.mekano.infrastructure.mapper.UserEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
 *   <li>{@code save()} — sem {@code @Transactional}: a transação é aberta pelo chamador
 *       ({@code CreateUserUseCase.execute()} via {@code @Transactional}).</li>
 *   <li>{@code markAsDeleted()} — {@code @Transactional}: necessário para operação de escrita.</li>
 *   <li>Métodos de leitura — sem {@code @Transactional}: Quarkus/JPA gerencia automaticamente
 *       se houver transação ativa no chamador</li>
 * </ul>
 *
 * <p>Tolerância a falhas (Fase 7):
 * <ul>
 *   <li>{@code findByEmail} e {@code findById} têm {@code @Retry} (3 tentativas) — leituras são
 *       idempotentes e seguras para retry; falhas transientes (conexão derrubada, deadlock) são
 *       absorvidas sem propagar ao caller.</li>
 *   <li>{@code save} tem {@code @Timeout(5s)} mas <strong>não</strong> {@code @Retry} (D-03):
 *       retry em escrita arrisca duplicate INSERT (caso a primeira tentativa tenha persistido
 *       a linha mas falhado pós-flush) e é antipattern com {@code @Transactional} — a TX abre
 *       UMA vez (interceptor mais externo), então retries operariam dentro da mesma TX já
 *       marcada para rollback.</li>
 *   <li><strong>Sem {@code @CircuitBreaker}</strong> (D-02): o PostgreSQL local
 *       (DevServices/docker-compose) não é um serviço externo flaky. Circuit breaker
 *       introduziria estado adicional e novo ponto de falha sem benefício neste contexto.
 *       A omissão é deliberada, não esquecimento.</li>
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
     * <p><strong>Nota sobre {@code @Transactional} removido (D-03):</strong> A responsabilidade
     * da transação foi movida para {@code CreateUserUseCase.execute()} — a unidade de trabalho
     * pertence ao use case, não ao repositório.
     *
     * <p><strong>Nota sobre {@code @Timeout} + JDBC (D-04):</strong> a {@code TimeoutException}
     * é lançada no momento correto pelo interceptor MP-FT, mas o driver pgjdbc só checa o flag
     * de interrupt da thread em pontos de I/O — a query no servidor PostgreSQL pode continuar
     * executando até completar ou falhar naturalmente. Para um INSERT simples como este, o
     * impacto é desprezível; para queries longas (não é o caso aqui), considerar configurar
     * {@code statement_timeout} no datasource para cancelamento server-side.
     *
     * @param user entidade de domínio a ser salva
     * @return entidade de domínio reconstruída após persistência
     */
    @Override
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public User save(User user) {
        var entity = mapper.toEntity(user);
        panacheRepository.persist(entity);
        panacheRepository.flush();
        return mapper.toDomain(entity);
    }

    /**
     * Busca usuário ativo pelo UUID (isActive = true).
     *
     * <p>Usa HQL explícito com filtro de soft delete para garantir que registros
     * deletados logicamente não sejam retornados. O built-in {@code findByIdOptional}
     * do Panache não aplica filtros — por isso usamos HQL customizado.
     *
     * @param id UUID do usuário
     * @return Optional com o User se encontrado e ativo, ou Optional.empty()
     */
    @Override
    @Retry(maxRetries = 3)
    public Optional<User> findById(UUID id) {
        return panacheRepository.find("id = ?1 AND isActive = ?2", id, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    /**
     * Busca usuário ativo pelo email (isActive = true).
     *
     * <p>Usa HQL com filtro de soft delete: {@code email = ?1 AND isActive = ?2}.
     * Registros deletados logicamente são excluídos dos resultados.
     *
     * @param email string do email (normalizada)
     * @return Optional com o User se encontrado e ativo, ou Optional.empty()
     */
    @Override
    @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
    public Optional<User> findByEmail(String email) {
        return panacheRepository.find("email = ?1 AND isActive = ?2", email, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    /**
     * Verifica existência de usuário ativo pelo email usando COUNT.
     *
     * <p>Mais eficiente que {@code findByEmail().isPresent()} — não carrega a entidade completa.
     * Inclui filtro de soft delete ({@code isActive = true}) para consistência.
     *
     * @param email string do email
     * @return true se existe usuário ativo com esse email, false caso contrário
     */
    @Override
    public boolean existsByEmail(String email) {
        return panacheRepository.count("email = ?1 AND isActive = ?2", email, true) > 0;
    }

    /**
     * Marca um usuário como deletado (soft delete).
     *
     * <p>Define {@code deletedAt = now()} e {@code isActive = false} no registro.
     * O registro permanece no banco mas é excluído de todas as queries de
     * leitura que usam {@code isActive = true}.
     *
     * <p>Usa {@code findByIdOptional()} do Panache (busca sem filtro de soft delete)
     * para localizar o registro independentemente do estado atual.
     *
     * @param id UUID do usuário a marcar como deletado
     * @throws UserNotFoundException se o UUID não existir
     */
    @Override
    @Transactional
    public void markAsDeleted(UUID id) {
        UserEntity entity = panacheRepository.findByIdOptional(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
    }
}
