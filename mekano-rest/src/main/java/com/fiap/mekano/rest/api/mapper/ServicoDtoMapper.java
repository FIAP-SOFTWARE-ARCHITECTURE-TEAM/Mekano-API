package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.port.in.CreateServicoCommand;
import com.fiap.mekano.domain.port.in.UpdateServicoCommand;
import com.fiap.mekano.rest.api.dto.CreateServicoRequest;
import com.fiap.mekano.rest.api.dto.ServicoResponse;
import com.fiap.mekano.rest.api.dto.UpdateServicoRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct entre DTOs HTTP e tipos de domínio para Servico.
 *
 * <p>componentModel = "cdi" para injeção via CDI.
 * Mapeamentos diretos — campos têm mesmo nome e tipo.
 */
@Mapper(componentModel = "cdi")
public interface ServicoDtoMapper {

    CreateServicoCommand toCreateCommand(CreateServicoRequest request);

    UpdateServicoCommand toUpdateCommand(UpdateServicoRequest request);

    @Mapping(target = "id", source = "id")
    ServicoResponse toResponse(Servico servico);
}
