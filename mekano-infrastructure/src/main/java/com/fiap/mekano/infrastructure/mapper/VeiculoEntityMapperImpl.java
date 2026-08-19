package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.infrastructure.entity.VeiculoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VeiculoEntityMapperImpl implements VeiculoEntityMapper {

    @Inject
    PlacaVeiculoMapper placaMapper;

    @Override
    public VeiculoEntity toEntity(Veiculo veiculo) {
        if (veiculo == null) {
            return null;
        }

        VeiculoEntity entity = new VeiculoEntity();

        entity.setUuid(veiculo.getId());
        entity.setClienteUuid(veiculo.getClienteUuid());

        entity.setPlaca(
                placaMapper.placaToString(
                        veiculo.getPlaca()));

        entity.setMarca(veiculo.getMarca());
        entity.setModelo(veiculo.getModelo());
        entity.setAno(veiculo.getAno());

        entity.setCreatedAt(
                veiculo.getCreatedAt());

        return entity;
    }

    @Override
    public Veiculo toDomain(VeiculoEntity entity) {

        if (entity == null) {
            return null;
        }

        return Veiculo.reconstitute(
                entity.getUuid(),
                entity.getClienteUuid(),
                entity.getPlaca(),
                entity.getMarca(),
                entity.getModelo(),
                entity.getAno(),
                entity.getCreatedAt(),
                entity.getIsActive());
    }

    @Override
    public void updateEntity(Veiculo veiculo, VeiculoEntity entity) {
        if (veiculo == null || entity == null) {
            return;
        }
        entity.setMarca(veiculo.getMarca());
        entity.setModelo(veiculo.getModelo());
        entity.setAno(veiculo.getAno());
    }
}
