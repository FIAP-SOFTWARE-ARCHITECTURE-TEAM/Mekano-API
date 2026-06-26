package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.infrastructure.entity.RequisicaoCompraEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface RequisicaoCompraEntityMapper {
    RequisicaoCompra toRequisicaoCompra(RequisicaoCompraEntity entity);
    RequisicaoCompraEntity toRequisicaoCompraEntity(RequisicaoCompra requisicao);
}
