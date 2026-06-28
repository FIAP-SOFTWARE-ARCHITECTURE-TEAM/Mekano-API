package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.rest.api.dto.CreateRequisicaoCompraRequest;
import com.fiap.mekano.rest.api.dto.RequisicaoCompraResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface RequisicaoCompraDtoMapper {

    CreateRequisicaoCompraCommand toCreateCommand(CreateRequisicaoCompraRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", expression = "java(requisicao.getStatus().name())")
    RequisicaoCompraResponse toResponse(RequisicaoCompra requisicao);
}
