package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.in.ItemRequisicaoCompraCommand;
import com.fiap.mekano.rest.api.dto.CreateRequisicaoCompraRequest;
import com.fiap.mekano.rest.api.dto.ItemRequisicaoCompraRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface RequisicaoCompraDtoMapper {

    CreateRequisicaoCompraCommand toCreateCommand(CreateRequisicaoCompraRequest request);

    default ItemRequisicaoCompraCommand toItemCommand(ItemRequisicaoCompraRequest request) {
        return new ItemRequisicaoCompraCommand(request.getPecaUuid(), request.getQuantidade());
    }

    default List<ItemRequisicaoCompraCommand> toItemCommands(List<ItemRequisicaoCompraRequest> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toItemCommand).toList();
    }
}
