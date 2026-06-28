package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.rest.api.dto.NfEntradaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface NfEntradaDtoMapper {

    @Mapping(target = "id", source = "id")
    NfEntradaResponse toResponse(NfEntrada nfEntrada);
}
