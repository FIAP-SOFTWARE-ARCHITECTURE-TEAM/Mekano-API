package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import com.fiap.mekano.infrastructure.entity.RequisicaoCompraEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraRepositoryImpl implements RequisicaoCompraRepositoryPort {

    private final RequisicaoCompraPanacheRepository panacheRepository;

    public RequisicaoCompraRepositoryImpl(RequisicaoCompraPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public RequisicaoCompra salvar(RequisicaoCompra requisicao) {
        RequisicaoCompraEntity entity = panacheRepository.find("uuid = ?1", requisicao.getId()).firstResult();
        if (entity == null) {
            entity = new RequisicaoCompraEntity();
        }

        entity.uuid = requisicao.getId();
        entity.pecaId = requisicao.getPecaId();
        entity.quantidade = requisicao.getQuantidade().intValue();
        entity.status = requisicao.getStatus().name();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(requisicao.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity);
    }

    @Override
    public Optional<RequisicaoCompra> buscarPorId(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(RequisicaoCompraRepositoryImpl::toDomain);
    }

    @Override
    public RequisicaoCompra atualizar(RequisicaoCompra requisicao) {
        return salvar(requisicao);
    }

    private static RequisicaoCompra toDomain(RequisicaoCompraEntity entity) {
        return RequisicaoCompra.reconstitute(
                entity.uuid,
                entity.pecaId,
                entity.quantidade == null ? 0L : entity.quantidade.longValue(),
                parseStatus(entity.status),
                "Requisicao " + (entity.getId() == null ? "0" : entity.getId()),
                entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt()
        );
    }

    private static StatusRequisicao parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return StatusRequisicao.ABERTA;
        }

        try {
            return StatusRequisicao.valueOf(status.strip().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return StatusRequisicao.ABERTA;
        }
    }
}
