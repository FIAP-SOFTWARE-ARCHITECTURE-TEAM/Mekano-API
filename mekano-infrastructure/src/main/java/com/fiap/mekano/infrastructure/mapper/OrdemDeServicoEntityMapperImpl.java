package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper manual OrdemDeServico ↔ OrdemDeServicoEntity.
 * StatusOS ↔ String conversão explícita.
 */
@ApplicationScoped
public class OrdemDeServicoEntityMapperImpl implements OrdemDeServicoEntityMapper {

    @Override
    public OrdemDeServicoEntity toEntity(OrdemDeServico os) {
        if (os == null) return null;
        OrdemDeServicoEntity entity = new OrdemDeServicoEntity();
        entity.setUuid(os.getId());
        entity.setClienteId(os.getClienteId());
        entity.setVeiculoId(os.getVeiculoId());
        entity.setDescricaoProblema(os.getDescricaoProblema());
        entity.setStatus(os.getStatus().name());
        entity.setMotivoCancelamento(os.getMotivoCancelamento());
        entity.setCreatedAt(os.getCreatedAt());
        entity.setVersion(os.getVersion());
        return entity;
    }

    @Override
    public OrdemDeServico toDomain(OrdemDeServicoEntity entity) {
        if (entity == null) return null;
        return OrdemDeServico.reconstitute(
                entity.getUuid(),
                entity.getClienteId(),
                entity.getVeiculoId(),
                entity.getDescricaoProblema(),
                StatusOS.valueOf(entity.getStatus()),
                entity.getMotivoCancelamento(),
                entity.getCreatedAt(),
                entity.getVersion()
        );
    }
}
