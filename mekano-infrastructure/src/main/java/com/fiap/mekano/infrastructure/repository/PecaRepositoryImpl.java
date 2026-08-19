package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.PecaEntity;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PecaRepositoryImpl implements PecaRepositoryPort {

    private final PecaPanacheRepository panacheRepository;
    private final EntityManager em;

    public PecaRepositoryImpl(PecaPanacheRepository panacheRepository, EntityManager em) {
        this.panacheRepository = panacheRepository;
        this.em = em;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public Peca salvar(Peca peca) {
        PecaEntity entity = panacheRepository.find("uuid = ?1", peca.getId()).firstResult();
        if (entity == null) {
            entity = new PecaEntity();
        }

        entity.uuid = peca.getId();
        entity.codigo = peca.getCodigo();
        entity.descricao = peca.getDescricao();
        entity.valorUnitario = peca.getValorUnitario();
        entity.saldo = peca.getSaldoAtual() == null ? 0 : peca.getSaldoAtual().intValue();
        entity.saldoReservado = peca.getSaldoReservado() == null ? 0 : peca.getSaldoReservado().intValue();
        entity.estoqueMinimo = peca.getEstoqueMinimo() == null ? 0 : peca.getEstoqueMinimo().intValue();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(peca.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity);
    }

    @Override
    @CacheResult(cacheName = CacheNames.PECAS)
    public Optional<Peca> buscarPorId(UUID id) {
        return panacheRepository.find("uuid = ?1", id)
                .firstResultOptional()
                .map(PecaRepositoryImpl::toDomain);
    }

    @Override
    public List<Peca> findAll(int page, int size) {
        return panacheRepository.findAll(Sort.by("id"))
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(PecaRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count();
    }

    @Override
    public List<Peca> listarAbaixoEstoqueMinimo() {
        return panacheRepository.find("isActive = ?1", true).list()
                .stream()
                .filter(e -> e.estoqueMinimo > 0 && (e.saldo - e.saldoReservado) < e.estoqueMinimo)
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
                "UPDATE pecas SET saldo = saldo - :qtd WHERE uuid = :uuid AND saldo >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .executeUpdate();
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public void creditarSaldo(UUID uuid, Integer quantidade) {
        em.createNativeQuery(
                "UPDATE pecas SET saldo = saldo + :qtd WHERE uuid = :uuid"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .executeUpdate();
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public boolean reservarSaldo(UUID uuid, Integer quantidade) {
        int rowsUpdated = em.createNativeQuery(
                "UPDATE pecas SET saldo_reservado = saldo_reservado + :qtd WHERE uuid = :uuid AND (saldo - saldo_reservado) >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .executeUpdate();
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public boolean debitarSaldoReservado(UUID uuid, Integer quantidade) {
        int rowsUpdated = em.createNativeQuery(
                "UPDATE pecas SET saldo = saldo - :qtd, saldo_reservado = saldo_reservado - :qtd WHERE uuid = :uuid AND saldo_reservado >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
                .executeUpdate();
        return rowsUpdated > 0;
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public boolean liberarReserva(UUID uuid, Integer quantidade) {
        int rowsUpdated = em.createNativeQuery(
                "UPDATE pecas SET saldo_reservado = saldo_reservado - :qtd WHERE uuid = :uuid AND saldo_reservado >= :qtd"
        )
                .setParameter("uuid", uuid)
                .setParameter("qtd", quantidade)
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
                entity.uuid,
                entity.codigo,
                entity.descricao,
                entity.valorUnitario,
                entity.saldo == null ? 0L : entity.saldo.longValue(),
                entity.estoqueMinimo == null ? 0L : entity.estoqueMinimo.longValue(),
                entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt(),
                entity.saldoReservado == null ? 0L : entity.saldoReservado.longValue(),
                entity.getIsActive()
        );
    }
}
