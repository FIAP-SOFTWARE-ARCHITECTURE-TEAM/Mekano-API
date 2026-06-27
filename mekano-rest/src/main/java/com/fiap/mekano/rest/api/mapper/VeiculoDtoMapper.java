package com.fiap.mekano.rest.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.in.CreateVeiculoCommand;
import com.fiap.mekano.domain.port.in.UpdateVeiculoCommand;
import com.fiap.mekano.rest.api.dto.CreateVeiculoRequest;
import com.fiap.mekano.rest.api.dto.UpdateVeiculoRequest;
import com.fiap.mekano.rest.api.dto.VeiculoResponse;

@Mapper(componentModel = "cdi")
public interface VeiculoDtoMapper {

    CreateVeiculoCommand toCommand(
            CreateVeiculoRequest request);

    UpdateVeiculoCommand toCommand(
            UpdateVeiculoRequest request);

    @Mapping(target = "placa", expression = "java(veiculo.getPlaca().getValue())")
    VeiculoResponse toResponse(
            Veiculo veiculo);
}
