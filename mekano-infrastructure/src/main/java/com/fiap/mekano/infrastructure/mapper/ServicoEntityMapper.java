package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.infrastructure.entity.ServicoEntity;

/**
 * Mapper entre {@link Servico} (domain) e {@link ServicoEntity} (JPA).
 *
 * Implementação manual (CDI bean) usando factory methods do domain model.
 */
public interface ServicoEntityMapper {

    ServicoEntity toEntity(Servico servico);

    Servico toDomain(ServicoEntity entity);
}
