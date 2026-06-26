package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.PlacaVeiculo;
import jakarta.enterprise.context.ApplicationScoped;
import org.mapstruct.Named;

@ApplicationScoped
public class PlacaVeiculoMapper {

    @Named("placaToString")
    public String placaToString(PlacaVeiculo placa) {
        return placa == null
                ? null
                : placa.getValue();
    }

    @Named("stringToPlaca")
    public PlacaVeiculo stringToPlaca(String value) {
        return value == null
                ? null
                : new PlacaVeiculo(value);
    }
}
