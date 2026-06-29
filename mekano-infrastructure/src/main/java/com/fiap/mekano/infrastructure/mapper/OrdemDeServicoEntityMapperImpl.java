package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrdemDeServicoEntityMapperImpl implements OrdemDeServicoEntityMapper {

    @Override
    public OrdemDeServicoEntity toEntity(OrdemDeServico os) {
        if (os == null) {
            return null;
        }
        OrdemDeServicoEntity entity = new OrdemDeServicoEntity();
        entity.setUuid(os.getId());
        entity.setClienteUuid(os.getClienteId());
        entity.setVeiculoUuid(os.getVeiculoId());
        entity.setDescricaoProblema(os.getDescricaoProblema());
        entity.setStatus(os.getStatus().name());
        entity.setMotivoCancelamento(os.getMotivoCancelamento());
        entity.setOrcamentoUuid(null);
        entity.setMecanicoUuid(null);
        entity.setExecucaoIniciadaEm(null);
        entity.setExecucaoFinalizadaEm(null);
        entity.setObservacaoExecucao(null);
        entity.setDataAprovacao(null);
        entity.setCreatedAt(os.getCreatedAt());
        entity.setVersion(os.getVersion());
        return entity;
    }

    @Override
    public OrdemDeServico toDomain(OrdemDeServicoEntity entity) {
        if (entity == null) {
            return null;
        }
        return OrdemDeServico.reconstitute(
                entity.getUuid(),
                entity.getClienteUuid(),
                entity.getVeiculoUuid(),
                entity.getDescricaoProblema(),
                StatusOS.valueOf(entity.getStatus()),
                entity.getMotivoCancelamento(),
                entity.getCreatedAt(),
                entity.getVersion()
        );
    }
}
