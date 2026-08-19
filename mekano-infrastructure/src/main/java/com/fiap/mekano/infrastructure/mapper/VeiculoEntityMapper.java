package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.infrastructure.entity.VeiculoEntity;

public interface VeiculoEntityMapper {

    VeiculoEntity toEntity(Veiculo veiculo);

    Veiculo toDomain(VeiculoEntity entity);

    void updateEntity(Veiculo veiculo, VeiculoEntity entity);
}