package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.infrastructure.entity.OrcamentoEntity;

public interface OrcamentoEntityMapper {

    OrcamentoEntity toEntity(Orcamento orcamento);

    Orcamento toDomain(OrcamentoEntity entity);
}
