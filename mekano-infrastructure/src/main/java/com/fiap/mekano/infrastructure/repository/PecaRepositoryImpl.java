package com.fiap.mekano.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.audit.AuditoriaOrigem;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.PecaEntity;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Page;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PecaRepositoryImpl implements PecaRepositoryPort {

    private final PecaPanacheRepository panacheRepository;
    private final EntityManager em;

    private static final UUID SISTEMA_UUID = AuditoriaOrigem.SISTEMA.getCodigo();

    public PecaRepositoryImpl(PecaPanacheRepository panacheRepository, EntityManager em) {
        this.panacheRepository = panacheRepository;
        this.em = em;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public Peca save(Peca peca) {
        PecaEntity entity = panacheRepository.find("uuid = ?1", peca.getId()).firstResult();
        if (entity == null) {
            entity = new PecaEntity();
            entity.setDeletedAt(null);
            entity.setIsActive(true);
        }

        entity.setUuid(peca.getId());
        entity.setCodigo(peca.getCodigo());
        entity.setDescricao(peca.getDescricao());
        entity.setValorUnitario(peca.getValorUnitario());
        entity.setSaldo(peca.getSaldoAtual() == null ? 0 : peca.getSaldoAtual().intValue());
        entity.setSaldoReservado(peca.getSaldoReservado() == null ? 0 : peca.getSaldoReservado().intValue());
        entity.setEstoqueMinimo(peca.getEstoqueMinimo() == null ? 0 : peca.getEstoqueMinimo().intValue());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(peca.getCreatedAt());
        }

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity);
    }

    @Override
    @CacheResult(cacheName = CacheNames.PECAS)
    public Optional<Peca> findById(UUID id) {
        return panacheRepository.find("uuid = ?1", id)
                .firstResultOptional()
                .map(PecaRepositoryImpl::toDomain);
    }

    @Override
    public List<Peca> findAll(int page, int size, Boolean isActive) {
        PanacheQuery<PecaEntity> query = isActive == null
                ? panacheRepository.findAll(Sort.by("id"))
                : panacheRepository.find("isActive = ?1", Sort.by("id"), isActive);
        return query.page(Page.of(page, size))
                .list()
                .stream()
                .map(PecaRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public long countAll(Boolean isActive) {
        return isActive == null
                ? panacheRepository.count()
                : panacheRepository.count("isActive = ?1", isActive);
    }

    @Override
    public List<Peca> listarAbaixoEstoqueMinimo() {
        return panacheRepository.find("isActive = ?1", true).list()
                .stream()
                .filter(e -> e.getEstoqueMinimo() > 0 && (e.getSaldo() - e.getSaldoReservado()) < e.getEstoqueMinimo())
                .map(PecaRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<Peca> buscarPorDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return Optional.empty();
        }

        return panacheRepository.find("descricao = ?1 and isActive = ?2", descricao.strip(), true)
                .firstResultOptional()
                .map(PecaRepositoryImpl::toDomain);
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public boolean debitarSaldo(UUID uuid, Integer quantidade) {
        int rowsUpdated = em.createNativeQuery(
                "UPDATE pecas SET saldo = saldo - :qtd, updated_by = :sistemaUuid WHERE uuid = :uuid AND saldo >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .setParameter("sistemaUuid", SISTEMA_UUID)
                .executeUpdate();
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public void creditarSaldo(UUID uuid, Integer quantidade) {
        em.createNativeQuery(
                "UPDATE pecas SET saldo = saldo + :qtd, updated_by = :sistemaUuid WHERE uuid = :uuid"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .setParameter("sistemaUuid", SISTEMA_UUID)
                .executeUpdate();
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public boolean reservarSaldo(UUID uuid, Integer quantidade) {
        int rowsUpdated = em.createNativeQuery(
                "UPDATE pecas SET saldo_reservado = saldo_reservado + :qtd, updated_by = :sistemaUuid WHERE uuid = :uuid AND (saldo - saldo_reservado) >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .setParameter("sistemaUuid", SISTEMA_UUID)
                .executeUpdate();
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public boolean debitarSaldoReservado(UUID uuid, Integer quantidade) {
        int rowsUpdated = em.createNativeQuery(
                "UPDATE pecas SET saldo = saldo - :qtd, saldo_reservado = saldo_reservado - :qtd, updated_by = :sistemaUuid WHERE uuid = :uuid AND saldo_reservado >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .setParameter("sistemaUuid", SISTEMA_UUID)
                .executeUpdate();
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public boolean liberarReserva(UUID uuid, Integer quantidade) {
        int rowsUpdated = em.createNativeQuery(
                "UPDATE pecas SET saldo_reservado = saldo_reservado - :qtd, updated_by = :sistemaUuid WHERE uuid = :uuid AND saldo_reservado >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .setParameter("sistemaUuid", SISTEMA_UUID)
                .executeUpdate();
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    public void remover(UUID id) {
        panacheRepository.find("uuid = ?1", id).firstResultOptional().ifPresent(entity -> {
            entity.setIsActive(false);
            entity.setDeletedAt(LocalDateTime.now());
            panacheRepository.flush();
        });
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public void reativar(UUID id) {
        PecaEntity entity = panacheRepository.find("uuid = ?1", id)
                .firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("peca.not.found", id)));
        entity.setIsActive(true);
        entity.setDeletedAt(null);
        panacheRepository.flush();
    }

    private static Peca toDomain(PecaEntity entity) {
        return Peca.reconstitute(
                entity.getUuid(),
                entity.getCodigo(),
                entity.getDescricao(),
                entity.getValorUnitario(),
                entity.getSaldo() == null ? 0L : entity.getSaldo().longValue(),
                entity.getEstoqueMinimo() == null ? 0L : entity.getEstoqueMinimo().longValue(),
                entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt(),
                entity.getSaldoReservado() == null ? 0L : entity.getSaldoReservado().longValue(),
                entity.getIsActive()
        );
    }
}
