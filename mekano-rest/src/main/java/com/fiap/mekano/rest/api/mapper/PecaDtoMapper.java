package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.rest.api.dto.CreatePecaRequest;
import com.fiap.mekano.rest.api.dto.PecaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface PecaDtoMapper {

    CreatePecaCommand toCreateCommand(CreatePecaRequest request);

    @Mapping(target = "id", source = "id")
    PecaResponse toResponse(Peca peca);
}
