package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.domain.port.in.UpdatePecaCommand;
import com.fiap.mekano.rest.api.dto.CreatePecaRequest;
import com.fiap.mekano.rest.api.dto.PecaResponse;
import com.fiap.mekano.rest.api.dto.UpdatePecaRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "cdi")
public interface PecaDtoMapper {

    CreatePecaCommand toCreateCommand(CreatePecaRequest request);

    UpdatePecaCommand toUpdateCommand(UUID id, UpdatePecaRequest request);

    @Mapping(target = "id", source = "id")
    PecaResponse toResponse(Peca peca);
}
