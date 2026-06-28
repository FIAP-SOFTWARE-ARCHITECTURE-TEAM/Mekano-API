package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.rest.api.dto.CreateRequisicaoCompraRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface RequisicaoCompraDtoMapper {

    CreateRequisicaoCompraCommand toCreateCommand(CreateRequisicaoCompraRequest request);
}
