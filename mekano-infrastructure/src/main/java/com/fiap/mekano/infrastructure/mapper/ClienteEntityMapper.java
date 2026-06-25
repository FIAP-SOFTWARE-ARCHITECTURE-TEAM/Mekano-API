package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.infrastructure.entity.ClienteEntity;

public interface ClienteEntityMapper {

    ClienteEntity toEntity(Cliente cliente);

    Cliente toDomain(ClienteEntity entity);
}
