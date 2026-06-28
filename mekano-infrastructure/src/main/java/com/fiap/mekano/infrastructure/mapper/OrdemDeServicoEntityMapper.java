package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;

public interface OrdemDeServicoEntityMapper {

    OrdemDeServicoEntity toEntity(OrdemDeServico ordemDeServico);

    OrdemDeServico toDomain(OrdemDeServicoEntity entity);
}
