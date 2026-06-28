package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.os.OrdemServico;
import com.fiap.mekano.domain.os.OsStatus;
import com.fiap.mekano.domain.port.out.OrdemServicoRepositoryPort;
import com.fiap.mekano.infrastructure.entity.OrdemServicoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrdemServicoRepositoryImpl implements OrdemServicoRepositoryPort {

    @Inject
    OrdemServicoPanacheRepository panacheRepository;

    @Override
    @Transactional
    public OrdemServico save(OrdemServico ordemServico) {
        OrdemServicoEntity entity = panacheRepository.findByUuid(ordemServico.getUuid())
                .orElseGet(OrdemServicoEntity::new);

        entity.uuid = ordemServico.getUuid();
        entity.status = ordemServico.getStatus().name();

        panacheRepository.persist(entity);

        return toDomain(entity);
    }

    @Override
    public Optional<OrdemServico> findByUuid(UUID uuid) {
        return panacheRepository.findByUuid(uuid)
                .map(this::toDomain);
    }

    private OrdemServico toDomain(OrdemServicoEntity entity) {
        return OrdemServico.restaurar(
                entity.uuid,
                OsStatus.valueOf(entity.status)
        );
    }
}