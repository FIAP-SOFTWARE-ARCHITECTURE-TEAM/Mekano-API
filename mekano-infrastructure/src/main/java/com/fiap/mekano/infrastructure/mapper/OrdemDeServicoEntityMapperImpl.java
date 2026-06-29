package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.model.StatusPagamento;
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
        entity.setOrcamentoUuid(os.getOrcamentoUuid());
        entity.setMecanicoUuid(os.getMecanicoUuid());
        entity.setExecucaoIniciadaEm(os.getExecucaoIniciadaEm());
        entity.setExecucaoFinalizadaEm(os.getExecucaoFinalizadaEm());
        entity.setObservacaoExecucao(os.getObservacaoExecucao());
        entity.setDataAprovacao(os.getDataAprovacao());
        entity.setCreatedAt(os.getCreatedAt());
        entity.setVersion(os.getVersion());
        // TODO(#33): mapear statusPagamento, valorCobrado, dataPagamento,
        //           dataEntrega, observacaoEntrega quando campos forem
        //           adicionados na OrdemDeServicoEntity
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
                entity.getOrcamentoUuid(),
                entity.getMecanicoUuid(),
                entity.getExecucaoIniciadaEm(),
                entity.getExecucaoFinalizadaEm(),
                entity.getObservacaoExecucao(),
                entity.getDataAprovacao(),
                null, // TODO(#33): statusPagamento da entity
                null, // TODO(#33): valorCobrado da entity
                null, // TODO(#33): dataPagamento da entity
                null, // TODO(#33): dataEntrega da entity
                null, // TODO(#33): observacaoEntrega da entity
                entity.getCreatedAt(),
                entity.getVersion()
        );
    }
}
