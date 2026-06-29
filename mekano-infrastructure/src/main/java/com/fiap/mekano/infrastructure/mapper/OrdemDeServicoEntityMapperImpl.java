package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.os.StatusEntrega;
import com.fiap.mekano.domain.os.StatusPagamento;
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
        
        entity.setStatusPagamento(
                os.getStatusPagamento() == null
                        ? StatusPagamento.NAO_COBRADO.name()
                        : os.getStatusPagamento().name()
        );

        entity.setStatusEntrega(
                os.getStatusEntrega() == null
                        ? StatusEntrega.NAO_LIBERADA.name()
                        : os.getStatusEntrega().name()
        );

        entity.setCobrancaGeradaEm(os.getCobrancaGeradaEm());
        entity.setPagamentoConfirmadoEm(os.getPagamentoConfirmadoEm());
        entity.setReferenciaPagamento(os.getReferenciaPagamento());
        entity.setEntregueEm(os.getEntregueEm());
        entity.setRecebidoPor(os.getRecebidoPor());
        
        
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
                parseStatusPagamento(entity.getStatusPagamento()), // TODO(#33): substituir por entity.getStatusPagamento() quando StatusPagamento for mapeado na entity
                null, // TODO(#33): valorCobrado da entity
                null, // TODO(#33): dataPagamento da entity
                null, // TODO(#33): dataEntrega da entity
                null, // TODO(#33): observacaoEntrega da entity
                parseStatusEntrega(entity.getStatusEntrega()),
                entity.getCobrancaGeradaEm(),
                entity.getPagamentoConfirmadoEm(),
                entity.getReferenciaPagamento(),
                entity.getEntregueEm(),
                entity.getRecebidoPor(),
                entity.getCreatedAt(),
                entity.getVersion()
                
        );
    }
    
    private StatusPagamento parseStatusPagamento(String value) {
        if (value == null || value.isBlank()) {
            return StatusPagamento.NAO_COBRADO;
        }

        return StatusPagamento.valueOf(value);
    }

    private StatusEntrega parseStatusEntrega(String value) {
        if (value == null || value.isBlank()) {
            return StatusEntrega.NAO_LIBERADA;
        }

        return StatusEntrega.valueOf(value);
    }
}
