package com.fiap.mekano.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.out.ItemRequisicaoCompraRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import com.fiap.mekano.infrastructure.entity.RequisicaoCompraEntity;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RequisicaoCompraRepositoryImpl implements RequisicaoCompraRepositoryPort {

    private final RequisicaoCompraPanacheRepository panacheRepository;
    private final ItemRequisicaoCompraRepositoryPort itemRepository;

    public RequisicaoCompraRepositoryImpl(RequisicaoCompraPanacheRepository panacheRepository,
                                          ItemRequisicaoCompraRepositoryPort itemRepository) {
        this.panacheRepository = panacheRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public RequisicaoCompra save(RequisicaoCompra requisicao) {
        RequisicaoCompraEntity entity = panacheRepository.find("uuid = ?1", requisicao.getId()).firstResult();
        if (entity == null) {
            entity = new RequisicaoCompraEntity();
        }

        entity.setUuid(requisicao.getId());
        entity.setStatus(requisicao.getStatus().name());
        entity.setMotivo(requisicao.getMotivo().name());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(requisicao.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity, requisicao.getItens());
    }

    @Override
    public Optional<RequisicaoCompra> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(entity -> {
                    List<ItemRequisicaoCompra> itens = itemRepository.findByRequisicaoCompraId(entity.getUuid());
                    return toDomain(entity, itens);
                });
    }

    @Override
    public List<RequisicaoCompra> findAll(int page, int size) {
        return panacheRepository.find("isActive = ?1", Sort.by("id"), true)
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(entity -> {
                    List<ItemRequisicaoCompra> itens = itemRepository.findByRequisicaoCompraId(entity.getUuid());
                    return toDomain(entity, itens);
                })
                .toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive", true);
    }

    @Override
    public RequisicaoCompra atualizar(RequisicaoCompra requisicao) {
        return save(requisicao);
    }

    private static RequisicaoCompra toDomain(RequisicaoCompraEntity entity, List<ItemRequisicaoCompra> itens) {
        return RequisicaoCompra.reconstitute(
                entity.getUuid(),
                itens == null ? new ArrayList<>() : itens,
                parseStatus(entity.getStatus()),
                parseMotivo(entity.getMotivo()),
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

    private static MotivoRequisicao parseMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return MotivoRequisicao.ESTOQUE_MINIMO;
        }

        try {
            return MotivoRequisicao.valueOf(motivo.strip().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return MotivoRequisicao.ESTOQUE_MINIMO;
        }
    }
}
