package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;

public interface OrdemDeServicoEntityMapper {
    OrdemDeServicoEntity toEntity(OrdemDeServico os);
    OrdemDeServico toDomain(OrdemDeServicoEntity entity);
}
