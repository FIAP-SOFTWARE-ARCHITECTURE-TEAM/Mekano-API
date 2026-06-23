package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.in.CreateClienteCommand;
import com.fiap.mekano.domain.port.in.UpdateClienteCommand;
import com.fiap.mekano.rest.api.dto.ClienteResponse;
import com.fiap.mekano.rest.api.dto.CreateClienteRequest;
import com.fiap.mekano.rest.api.dto.UpdateClienteRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface ClienteDtoMapper {

    CreateClienteCommand toCommand(CreateClienteRequest request);

    UpdateClienteCommand toCommand(UpdateClienteRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "cpf", expression = "java(cliente.getCpf().getValue())")
    @Mapping(target = "email", expression = "java(cliente.getEmail().getValue())")
    @Mapping(target = "telefone", expression = "java(cliente.getTelefone().getValue())")
    ClienteResponse toResponse(Cliente cliente);
}
