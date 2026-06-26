package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.infrastructure.entity.PecaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface PecaEntityMapper {
    Peca toPeca(PecaEntity entity);
    PecaEntity toPecaEntity(Peca peca);
}
