package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.UnidadeMedida;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.entity.PecaEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PecaRepositoryImpl implements PecaRepositoryPort {

    private final PecaPanacheRepository panacheRepository;

    public PecaRepositoryImpl(PecaPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public Peca salvar(Peca peca) {
        PecaEntity entity = panacheRepository.find("uuid = ?1", peca.getId()).firstResult();
        if (entity == null) {
            entity = new PecaEntity();
        }

        entity.uuid = peca.getId();
        entity.descricao = peca.getDescricao();
        entity.saldo = peca.getSaldoAtual() == null ? 0 : peca.getSaldoAtual().intValue();
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
    public Optional<Peca> buscarPorId(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(PecaRepositoryImpl::toDomain);
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

    private static Peca toDomain(PecaEntity entity) {
        return Peca.reconstitute(
                entity.uuid,
                entity.uuid.toString(),
                entity.descricao,
                UnidadeMedida.UNIDADE,
                BigDecimal.ZERO,
                entity.saldo == null ? 0L : entity.saldo.longValue(),
                entity.estoqueMinimo == null ? 0L : entity.estoqueMinimo.longValue(),
                entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt()
        );
    }
}
