package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.infrastructure.entity.ServicoEntity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Implementação CDI do mapper Servico ↔ ServicoEntity.
 *
 * <p>Usa factory methods do domain model para reconstrução ({@code Servico.reconstitute()}).
 * Sem VOs customizados — mapeamento direto de campos.
 */
@ApplicationScoped
public class ServicoEntityMapperImpl implements ServicoEntityMapper {

    @Override
    public ServicoEntity toEntity(Servico servico) {
        if (servico == null) {
            return null;
        }
        ServicoEntity entity = new ServicoEntity();
        entity.setUuid(servico.getId());
        entity.setNome(servico.getNome());
        entity.setDescricao(servico.getDescricao());
        entity.setValor(servico.getValor());
        entity.setCreatedAt(servico.getCreatedAt());
        return entity;
    }

    @Override
    public Servico toDomain(ServicoEntity entity) {
        if (entity == null) {
            return null;
        }
        return Servico.reconstitute(
                entity.getUuid(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getValor(),
                entity.getCreatedAt()
        );
    }
}
